package me.binarylife.mastaka.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import me.binarylife.mastaka.R;
import me.binarylife.mastaka.database.NoteDatabase;
import me.binarylife.mastaka.databinding.ActivityIotBinding;
import me.binarylife.mastaka.models.Note;

public class IotActivity extends AppCompatActivity {

    private static final String TAG = "IotActivity";

    // BLE CONFIG
    private static final String DEVICE_NAME = "Mastaka_IoT";
    private static final String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private static final String CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    private static final String END_MARKER = "\n<END>";

    private static final int REQUEST_BLE_PERMISSION = 1001;
    private static final int REQUEST_ENABLE_LOCATION = 1002;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1003;

    private static final long SCAN_TIMEOUT_MS = 15000;
    private static final long ACK_TIMEOUT_MS = 5000;
    private static final int REQUESTED_MTU = 185;

    private static final UUID SERVICE_ID = UUID.fromString(SERVICE_UUID);
    private static final UUID CHARACTERISTIC_ID = UUID.fromString(CHARACTERISTIC_UUID);
    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private ActivityIotBinding binding;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic noteCharacteristic;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isScanning = false;
    private boolean isConnected = false;
    private boolean isReady = false;
    private boolean isSending = false;
    private boolean awaitingAck = false;

    private int chunkSize = 20;

    private final Queue<byte[]> writeQueue = new ArrayDeque<>();
    private final Set<String> seenDevices = new HashSet<>();

    private final Runnable scanTimeoutRunnable = () -> {
        if (isScanning) {
            Log.w(TAG, "Scan timeout: device not found in " + SCAN_TIMEOUT_MS + "ms");
            stopBleScan();
            updateConnectionUI(false, "Disconnected");
            showToast("Device পাওয়া যায়নি! ESP32 চালু আছে কিনা দেখুন।");
        }
    };

    private final Runnable ackTimeoutRunnable = () -> {
        if (awaitingAck) {
            Log.e(TAG, "ACK timeout");
            awaitingAck = false;
            isSending = false;
            runOnUiThread(() -> binding.btnSync.setEnabled(true));
            showToast("ESP32 থেকে ACK আসেনি!");
        }
    };

    // ===============================
    // CREATE
    // ===============================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityIotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.iotToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.iotToolbar.setNavigationOnClickListener(v -> finish());

        BluetoothManager manager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        }

        binding.tvDeviceId.setText(DEVICE_NAME);
        updateConnectionUI(false, "Disconnected");

        binding.btnConnect.setOnClickListener(v -> {
            if (isConnected || bluetoothGatt != null) {
                disconnectIotDevice();
            } else if (isScanning) {
                stopBleScan();
                updateConnectionUI(false, "Disconnected");
            } else {
                connectToIotDevice();
            }
        });

        binding.btnSync.setOnClickListener(v -> {
            if (!isReady || bluetoothGatt == null) {
                showToast("Device not ready!");
                return;
            }
            syncNotesToEsp32();
        });

        fetchAndPrintLocalNotes();
    }

    // ===============================
    // PERMISSIONS
    // ===============================

    private String[] getRequiredPermissions() {
        List<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN);
            list.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        // MIUI সহ অনেক phone-এ Location permission ছাড়া scan কাজ করে না,
        // তাই সব Android version-এ চাওয়া হচ্ছে
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        return list.toArray(new String[0]);
    }

    private boolean hasBlePermissions() {
        for (String p : getRequiredPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestBlePermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_BLE_PERMISSION);
    }

    private boolean isLocationServiceOn() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return lm.isLocationEnabled();
        }
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Location বন্ধ থাকলে system dialog দেখিয়ে এক click-এ অন করানো
    private void askToEnableLocation() {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .build();

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .setAlwaysShow(true)
                .build();

        LocationServices.getSettingsClient(this)
                .checkLocationSettings(settingsRequest)
                .addOnSuccessListener(r -> {
                    Log.d(TAG, "Location already ON");
                    connectToIotDevice();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e)
                                    .startResolutionForResult(this, REQUEST_ENABLE_LOCATION);
                        } catch (IntentSender.SendIntentException ex) {
                            openLocationSettingsDialog();
                        }
                    } else {
                        openLocationSettingsDialog();
                    }
                });
    }

    // Fallback: Settings screen-এ পাঠানো
    private void openLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location দরকার")
                .setMessage("ESP32 খুঁজে পেতে Location অন থাকতে হবে।")
                .setPositiveButton("Settings", (d, w) ->
                        startActivityForResult(
                                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                REQUEST_ENABLE_LOCATION))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Permission চিরতরে deny হলে App Settings-এ পাঠানো
    private void openAppSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission দরকার")
                .setMessage("Bluetooth ও Location permission দিতে হবে। Settings থেকে Allow করুন।")
                .setPositiveButton("Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLE_PERMISSION) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            Log.d(TAG, "Permission result granted=" + granted);

            if (granted) {
                connectToIotDevice();   // বাকি চেক (BT, Location) ওখানেই হবে
            } else {
                boolean permanentlyDenied = false;
                for (String p : permissions) {
                    if (ActivityCompat.checkSelfPermission(this, p)
                            != PackageManager.PERMISSION_GRANTED
                            && !ActivityCompat.shouldShowRequestPermissionRationale(this, p)) {
                        permanentlyDenied = true;
                    }
                }
                if (permanentlyDenied) {
                    openAppSettingsDialog();
                } else {
                    showToast("Bluetooth ও Location permission লাগবে!");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_LOCATION) {
            if (isLocationServiceOn()) {
                connectToIotDevice();
            } else {
                showToast("Location অন না করলে ESP32 পাওয়া যাবে না!");
            }
        } else if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                connectToIotDevice();
            } else {
                showToast("Bluetooth অন করতে হবে!");
            }
        }
    }

    // ===============================
    // UI
    // ===============================

    private void updateConnectionUI(boolean connected, String statusText) {
        runOnUiThread(() -> {
            isConnected = connected;
            binding.tvConnectionStatus.setText(statusText);

            if (connected) {
                binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_synced);
                binding.btnConnect.setText("Disconnect");
                binding.btnSync.setEnabled(isReady && !isSending);
                binding.btnSync.setAlpha(isReady ? 1f : 0.5f);

            } else if (isScanning || statusText.contains("Scanning")
                    || statusText.contains("Connecting")) {
                binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_pending);
                binding.btnConnect.setText(statusText.contains("Scanning")
                        ? "Cancel Scan" : "Connecting...");
                binding.btnSync.setEnabled(false);
                binding.btnSync.setAlpha(0.5f);

            } else {
                binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_disconnected);
                binding.btnConnect.setText("Connect");
                binding.btnSync.setEnabled(false);
                binding.btnSync.setAlpha(0.5f);
            }
        });
    }

    private void showToast(String message) {
        Log.d(TAG, "TOAST: " + message);
        runOnUiThread(() ->
                Toast.makeText(IotActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "default_user";
    }

    // ===============================
    // START SCAN (সব চেক এখানে)
    // ===============================

    private void connectToIotDevice() {
        Log.d(TAG, "connectToIotDevice() SDK=" + Build.VERSION.SDK_INT);

        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported!");
            return;
        }

        // 1. Permission
        if (!hasBlePermissions()) {
            Log.d(TAG, "Permission missing, requesting...");
            requestBlePermissions();
            return;
        }

        // 2. Bluetooth অন কিনা
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth OFF, asking to enable");
            try {
                startActivityForResult(
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        REQUEST_ENABLE_BLUETOOTH);
            } catch (SecurityException e) {
                Log.e(TAG, "Enable BT error", e);
                showToast("Please turn on Bluetooth!");
            }
            return;
        }

        // 3. Location অন কিনা
        if (!isLocationServiceOn()) {
            Log.d(TAG, "Location OFF, asking to enable");
            askToEnableLocation();
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            showToast("BLE Scanner unavailable!");
            return;
        }

        if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt);
        }

        seenDevices.clear();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        isScanning = true;
        updateConnectionUI(false, "Scanning...");
        showToast("Searching for Mastaka IoT...");

        try {
            bluetoothLeScanner.startScan(null, settings, scanCallback);
            Log.d(TAG, "Scan started");
            handler.removeCallbacks(scanTimeoutRunnable);
            handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS);
        } catch (SecurityException e) {
            Log.e(TAG, "Scan permission error", e);
            isScanning = false;
            updateConnectionUI(false, "Disconnected");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.v(TAG, "RAW scan result: " + result.getDevice().getAddress());

            if (!isScanning) return;

            BluetoothDevice device = result.getDevice();
            ScanRecord record = result.getScanRecord();

            String name = record != null ? record.getDeviceName() : null;
            boolean uuidMatch = record != null
                    && record.getServiceUuids() != null
                    && record.getServiceUuids().contains(new ParcelUuid(SERVICE_ID));

            if (seenDevices.add(device.getAddress())) {
                Log.d(TAG, "Found: " + device.getAddress()
                        + " name=" + name + " rssi=" + result.getRssi()
                        + " uuidMatch=" + uuidMatch);
            }

            if (!DEVICE_NAME.equals(name) && !uuidMatch) return;

            Log.d(TAG, ">>> TARGET FOUND: " + device.getAddress());

            stopBleScan();
            updateConnectionUI(false, "Connecting...");
            showToast("Mastaka IoT Found!");

            // 133 error কমাতে একটু delay
            handler.postDelayed(() -> {
                try {
                    bluetoothGatt = device.connectGatt(
                            IotActivity.this, false, gattCallback,
                            BluetoothDevice.TRANSPORT_LE);
                    Log.d(TAG, "connectGatt() called");
                } catch (SecurityException e) {
                    Log.e(TAG, "GATT connection error", e);
                    updateConnectionUI(false, "Disconnected");
                }
            }, 300);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed: " + errorCode);
            isScanning = false;
            handler.removeCallbacks(scanTimeoutRunnable);
            updateConnectionUI(false, "Disconnected");
            showToast("BLE Scan Failed: " + errorCode);
        }
    };

    private void stopBleScan() {
        handler.removeCallbacks(scanTimeoutRunnable);

        if (bluetoothLeScanner != null && isScanning) {
            try {
                if (hasBlePermissions()) {
                    bluetoothLeScanner.stopScan(scanCallback);
                    Log.d(TAG, "Scan stopped");
                }
            } catch (Exception e) {
                Log.e(TAG, "Stop scan error", e);
            }
        }
        isScanning = false;
    }

    // ===============================
    // GATT CALLBACK
    // ===============================

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status=" + status + " newState=" + newState);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT error status: " + status
                        + (status == 133 ? " (generic error, retry করুন)" : ""));
                closeGatt(gatt);
                updateConnectionUI(false, "Disconnected");
                showToast("Connection failed: " + status);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to ESP32!");
                updateConnectionUI(true, "Discovering Services...");

                handler.postDelayed(() -> {
                    try {
                        boolean ok = gatt.requestMtu(REQUESTED_MTU);
                        Log.d(TAG, "requestMtu(" + REQUESTED_MTU + ") = " + ok);
                        if (!ok) gatt.discoverServices();
                    } catch (SecurityException e) {
                        Log.e(TAG, "MTU/discover error", e);
                    }
                }, 600);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "ESP32 disconnected.");
                closeGatt(gatt);
                updateConnectionUI(false, "Disconnected");
                showToast("ESP32 Disconnected!");
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged mtu=" + mtu + " status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                chunkSize = Math.max(20, mtu - 3);
            } else {
                chunkSize = 20;
            }
            Log.d(TAG, "chunkSize=" + chunkSize);
            try {
                gatt.discoverServices();
            } catch (SecurityException e) {
                Log.e(TAG, "Service discovery error", e);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered status=" + status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                showToast("Service discovery failed!");
                return;
            }

            for (BluetoothGattService s : gatt.getServices()) {
                Log.d(TAG, "Service: " + s.getUuid());
            }

            BluetoothGattService service = gatt.getService(SERVICE_ID);
            if (service == null) {
                showToast("IoT Service not found!");
                return;
            }

            noteCharacteristic = service.getCharacteristic(CHARACTERISTIC_ID);
            if (noteCharacteristic == null) {
                showToast("Characteristic not found!");
                return;
            }

            int properties = noteCharacteristic.getProperties();
            Log.d(TAG, "Characteristic properties=" + properties);

            boolean supportsWrite =
                    (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
            boolean supportsNotify =
                    (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;

            if (!supportsWrite || !supportsNotify) {
                showToast("ESP32 BLE properties are incorrect!");
                return;
            }

            enableNotifications(gatt);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite status=" + status);

            if (!CCCD_UUID.equals(descriptor.getUuid())) return;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                isReady = true;
                updateConnectionUI(true, "Connected & Ready");
                showToast("ESP32 Ready! You can sync notes.");
                Log.d(TAG, "Notifications enabled. READY.");
            } else {
                showToast("Notification setup failed: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (!CHARACTERISTIC_ID.equals(characteristic.getUuid())) return;

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Write failed status=" + status);
                isSending = false;
                synchronized (writeQueue) {
                    writeQueue.clear();
                }
                runOnUiThread(() -> binding.btnSync.setEnabled(true));
                showToast("BLE Write Failed: " + status);
                return;
            }

            synchronized (writeQueue) {
                writeQueue.poll();
            }
            writeNextChunk();
        }

        // Android 13+
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic,
                                            byte[] value) {
            handleNotification(characteristic, value);
        }

        // Android 12 ও নিচে
        @Override
        @SuppressWarnings("deprecation")
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleNotification(characteristic, characteristic.getValue());
            }
        }
    };

    private void handleNotification(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!CHARACTERISTIC_ID.equals(characteristic.getUuid()) || value == null) return;

        String response = new String(value, StandardCharsets.UTF_8);
        Log.d(TAG, "ESP32 Response: " + response);

        if ("ACK".equals(response)) {
            handler.removeCallbacks(ackTimeoutRunnable);
            awaitingAck = false;
            isSending = false;

            runOnUiThread(() -> {
                String currentTime = new SimpleDateFormat(
                        "hh:mm a, dd MMM yyyy", Locale.getDefault()).format(new Date());
                binding.tvLastSyncTime.setText(currentTime);
                binding.btnSync.setEnabled(true);
            });
            showToast("Notes synced successfully!");
        }
    }

    // ===============================
    // ENABLE NOTIFICATIONS
    // ===============================

    @SuppressWarnings("deprecation")
    private void enableNotifications(BluetoothGatt gatt) {
        if (!hasBlePermissions()) return;

        try {
            boolean ok = gatt.setCharacteristicNotification(noteCharacteristic, true);
            if (!ok) {
                showToast("Cannot enable notifications!");
                return;
            }

            BluetoothGattDescriptor descriptor = noteCharacteristic.getDescriptor(CCCD_UUID);
            if (descriptor == null) {
                showToast("BLE notification descriptor missing!");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int result = gatt.writeDescriptor(descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Log.d(TAG, "writeDescriptor result=" + result);
                if (result != BluetoothStatusCodes.SUCCESS) {
                    showToast("Descriptor write failed!");
                }
            } else {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                boolean r = gatt.writeDescriptor(descriptor);
                Log.d(TAG, "writeDescriptor result=" + r);
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission error", e);
        }
    }

    // ===============================
    // SYNC NOTES
    // ===============================

    private void syncNotesToEsp32() {
        if (!isReady || bluetoothGatt == null) {
            showToast("Device not ready!");
            return;
        }
        if (isSending) {
            showToast("Sync already in progress!");
            return;
        }

        isSending = true;
        awaitingAck = false;
        binding.btnSync.setEnabled(false);

        NoteDatabase db = NoteDatabase.getInstance(this);
        String userId = getCurrentUserId();

        new Thread(() -> {
            try {
                List<Note> noteList = db.noteDao().getNotesByUserSync(userId);

                final String REC_SEP = "\u001E";    // note এর মাঝে
                final String UNIT_SEP = "\u001F";   // title আর content এর মাঝে
                final int MAX_NOTES = 20;
                final int MAX_TITLE = 60;
                final int MAX_CONTENT = 1000;
                final int MAX_PAYLOAD_BYTES = 6000; // ESP32 buffer 8192 এর কম

                StringBuilder sb = new StringBuilder();
                int count = Math.min(noteList.size(), MAX_NOTES);

                for (int i = 0; i < count; i++) {
                    Note note = noteList.get(i);

                    String title = note.getTitle() != null ? note.getTitle() : "";
                    String desc = note.getDescription() != null ? note.getDescription() : "";

                    // separator character লেখার ভেতরে থাকলে মুছে ফেলা
                    title = title.replace(REC_SEP, "").replace(UNIT_SEP, "")
                            .replace("\n", " ").replace("\r", "").trim();
                    desc = desc.replace(REC_SEP, "").replace(UNIT_SEP, "")
                            .replace("\r", "").trim();

                    if (title.length() > MAX_TITLE) title = title.substring(0, MAX_TITLE);
                    if (desc.length() > MAX_CONTENT) desc = desc.substring(0, MAX_CONTENT);

                    String record = title + UNIT_SEP + desc;

                    // মোট size সীমা পার হলে আর যোগ হবে না
                    if (sb.toString().getBytes(StandardCharsets.UTF_8).length
                            + record.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
                        break;
                    }

                    if (sb.length() > 0) sb.append(REC_SEP);
                    sb.append(record);
                }

                if (sb.length() == 0) {
                    sb.append("No Notes").append(UNIT_SEP).append("Create a note in the app first.");
                }

                sb.append(END_MARKER);

                String payload = sb.toString();
                Log.d(TAG, "Payload (" + payload.length() + " chars)");

                prepareChunks(payload);

            } catch (Exception e) {
                Log.e(TAG, "Sync error", e);
                isSending = false;
                runOnUiThread(() -> binding.btnSync.setEnabled(true));
                showToast("Failed to read local notes!");
            }
        }).start();
    }

    private void prepareChunks(String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        synchronized (writeQueue) {
            writeQueue.clear();
            for (int i = 0; i < bytes.length; i += chunkSize) {
                int length = Math.min(chunkSize, bytes.length - i);
                byte[] chunk = new byte[length];
                System.arraycopy(bytes, i, chunk, 0, length);
                writeQueue.offer(chunk);
            }
            Log.d(TAG, "Prepared " + writeQueue.size() + " chunks, chunkSize=" + chunkSize
                    + ", total bytes=" + bytes.length);
        }

        runOnUiThread(() -> {
            showToast("Sending notes to ESP32...");
            writeNextChunk();
        });
    }

    @SuppressWarnings("deprecation")
    private void writeNextChunk() {
        if (bluetoothGatt == null || noteCharacteristic == null || !isReady) return;

        if (!hasBlePermissions()) {
            showToast("Bluetooth permission missing!");
            return;
        }

        byte[] nextChunk;
        synchronized (writeQueue) {
            nextChunk = writeQueue.peek();
        }

        if (nextChunk == null) {
            awaitingAck = true;
            Log.d(TAG, "All chunks sent. Waiting for ESP32 ACK.");
            handler.removeCallbacks(ackTimeoutRunnable);
            handler.postDelayed(ackTimeoutRunnable, ACK_TIMEOUT_MS);
            return;
        }

        try {
            boolean started;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int result = bluetoothGatt.writeCharacteristic(noteCharacteristic, nextChunk,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                started = (result == BluetoothStatusCodes.SUCCESS);
                if (!started) Log.e(TAG, "writeCharacteristic result=" + result);
            } else {
                noteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                noteCharacteristic.setValue(nextChunk);
                started = bluetoothGatt.writeCharacteristic(noteCharacteristic);
                if (!started) Log.e(TAG, "writeCharacteristic returned false");
            }

            if (!started) {
                isSending = false;
                showToast("Failed to write BLE chunk!");
                runOnUiThread(() -> binding.btnSync.setEnabled(true));
            }

        } catch (SecurityException e) {
            Log.e(TAG, "BLE write permission error", e);
            isSending = false;
            showToast("Bluetooth permission error!");
        }
    }

    // ===============================
    // DISCONNECT / CLOSE
    // ===============================

    private void disconnectIotDevice() {
        Log.d(TAG, "disconnectIotDevice()");

        stopBleScan();
        handler.removeCallbacks(ackTimeoutRunnable);

        isReady = false;
        isSending = false;
        awaitingAck = false;

        synchronized (writeQueue) {
            writeQueue.clear();
        }

        if (bluetoothGatt != null) {
            try {
                if (hasBlePermissions()) {
                    bluetoothGatt.disconnect();
                } else {
                    closeGatt(bluetoothGatt);
                    updateConnectionUI(false, "Disconnected");
                }
            } catch (Exception e) {
                Log.e(TAG, "Disconnect error", e);
                closeGatt(bluetoothGatt);
                updateConnectionUI(false, "Disconnected");
            }
        } else {
            updateConnectionUI(false, "Disconnected");
        }
    }

    private void closeGatt(BluetoothGatt gatt) {
        if (gatt == null) return;

        try {
            gatt.close();
        } catch (Exception e) {
            Log.e(TAG, "GATT close error", e);
        }

        if (bluetoothGatt == gatt) {
            bluetoothGatt = null;
            noteCharacteristic = null;
            isReady = false;
            isSending = false;
            awaitingAck = false;
            synchronized (writeQueue) {
                writeQueue.clear();
            }
        }
    }

    // ===============================
    // LOCAL NOTES DEBUG
    // ===============================

    private void fetchAndPrintLocalNotes() {
        NoteDatabase db = NoteDatabase.getInstance(this);
        String userId = getCurrentUserId();

        new Thread(() -> {
            List<Note> noteList = db.noteDao().getNotesByUserSync(userId);
            Log.d(TAG, "========== LOCAL NOTES (user=" + userId + ") ==========");
            for (Note note : noteList) {
                Log.d(TAG, "ID: " + note.getId() + " | Title: " + note.getTitle());
            }
            Log.d(TAG, "Total Notes: " + noteList.size());
        }).start();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopBleScan();
        isReady = false;

        if (bluetoothGatt != null) {
            BluetoothGatt oldGatt = bluetoothGatt;
            bluetoothGatt = null;
            try {
                if (hasBlePermissions()) oldGatt.disconnect();
                oldGatt.close();
            } catch (Exception e) {
                Log.e(TAG, "Destroy GATT error", e);
            }
        }
        super.onDestroy();
    }
}