package me.binarylife.mastaka.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;

import me.binarylife.mastaka.R;
import me.binarylife.mastaka.databinding.ActivityIotBinding;
import me.binarylife.mastaka.services.BleService;

public class IotActivity extends AppCompatActivity {

    private static final String TAG = "IotActivity";

    private static final int REQUEST_BLE_PERMISSION = 1001;
    private static final int REQUEST_ENABLE_LOCATION = 1002;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1003;

    private ActivityIotBinding binding;
    private BleService bleService;
    private boolean bound = false;
    private BluetoothAdapter bluetoothAdapter;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleService.LocalBinder binder = (BleService.LocalBinder) service;
            bleService = binder.getService();
            bound = true;
            Log.d(TAG, "BleService connected to IotActivity");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
            bound = false;
            Log.d(TAG, "BleService disconnected from IotActivity");
        }
    };

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

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        }

        binding.tvDeviceId.setText("Mastaka_IoT");

        binding.btnConnect.setOnClickListener(v -> {
            if (bound && bleService != null && bleService.isReady()) {
                bleService.disconnect();
            } else {
                checkPermissionsAndConnect();
            }
        });

        binding.btnSync.setOnClickListener(v -> {
            if (bound && bleService != null) {
                bleService.syncNotes();
            } else {
                Toast.makeText(this, "Service not bound!", Toast.LENGTH_SHORT).show();
            }
        });

        observeBleService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BleService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    private void observeBleService() {
        BleService.connectionStateLiveData.observe(this, state -> {
            updateConnectionUI(state);
        });

        BleService.isSendingLiveData.observe(this, isSending -> {
            boolean ready = bound && bleService != null && bleService.isReady();
            binding.btnSync.setEnabled(ready && !isSending);
            binding.btnSync.setAlpha(ready && !isSending ? 1f : 0.5f);
        });

        BleService.lastSyncTimeLiveData.observe(this, time -> {
            binding.tvLastSyncTime.setText(time);
        });

        BleService.toastMessageLiveData.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateConnectionUI(BleService.BleState state) {
        runOnUiThread(() -> {
            switch (state) {
                case READY:
                    binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_synced);
                    binding.tvConnectionStatus.setText("Connected & Ready");
                    binding.btnConnect.setText("Disconnect");
                    binding.btnSync.setEnabled(true);
                    binding.btnSync.setAlpha(1f);
                    break;
                case CONNECTED:
                    binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_pending);
                    binding.tvConnectionStatus.setText("Connected");
                    binding.btnConnect.setText("Disconnect");
                    binding.btnSync.setEnabled(false);
                    binding.btnSync.setAlpha(0.5f);
                    break;
                case CONNECTING:
                    binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_pending);
                    binding.tvConnectionStatus.setText("Connecting...");
                    binding.btnConnect.setText("Connecting...");
                    binding.btnSync.setEnabled(false);
                    binding.btnSync.setAlpha(0.5f);
                    break;
                case SCANNING:
                    binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_pending);
                    binding.tvConnectionStatus.setText("Scanning...");
                    binding.btnConnect.setText("Cancel Scan");
                    binding.btnSync.setEnabled(false);
                    binding.btnSync.setAlpha(0.5f);
                    break;
                case DISCONNECTED:
                default:
                    binding.viewStatusIndicator.setBackgroundResource(R.drawable.sync_dot_disconnected);
                    binding.tvConnectionStatus.setText("Disconnected");
                    binding.btnConnect.setText("Connect");
                    binding.btnSync.setEnabled(false);
                    binding.btnSync.setAlpha(0.5f);
                    break;
            }
        });
    }

    // ===============================
    // PERMISSIONS & CHECKS
    // ===============================

    private String[] getRequiredPermissions() {
        List<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN);
            list.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        return list.toArray(new String[0]);
    }

    private boolean hasBlePermissions() {
        for (String p : getRequiredPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
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
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void askToEnableLocation() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000).build();
        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .setAlwaysShow(true)
                .build();

        LocationServices.getSettingsClient(this)
                .checkLocationSettings(settingsRequest)
                .addOnSuccessListener(r -> checkPermissionsAndConnect())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(this, REQUEST_ENABLE_LOCATION);
                        } catch (IntentSender.SendIntentException ex) {
                            openLocationSettingsDialog();
                        }
                    } else {
                        openLocationSettingsDialog();
                    }
                });
    }

    private void openLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location দরকার")
                .setMessage("ESP32 খুঁজে পেতে Location অন থাকতে হবে।")
                .setPositiveButton("Settings", (d, w) ->
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLE_LOCATION))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAppSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission দরকার")
                .setMessage("Bluetooth, Notification ও Location permission দিতে হবে। Settings থেকে Allow করুন।")
                .setPositiveButton("Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkPermissionsAndConnect() {
        if (!hasBlePermissions()) {
            requestBlePermissions();
            return;
        }

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            try {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH);
            } catch (SecurityException e) {
                Toast.makeText(this, "Please turn on Bluetooth!", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!isLocationServiceOn()) {
            askToEnableLocation();
            return;
        }

        // Start Foreground Service and connect
        Intent serviceIntent = new Intent(this, BleService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        if (!bound) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        handlerStartConnect();
    }

    private void handlerStartConnect() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (bound && bleService != null) {
                bleService.startConnect();
            }
        }, 300);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSION) {
            boolean granted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                checkPermissionsAndConnect();
            } else {
                boolean permanentlyDenied = false;
                for (String p : permissions) {
                    if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, p)) {
                        permanentlyDenied = true;
                    }
                }
                if (permanentlyDenied) {
                    openAppSettingsDialog();
                } else {
                    Toast.makeText(this, "Permissions required!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_LOCATION) {
            if (isLocationServiceOn()) {
                checkPermissionsAndConnect();
            } else {
                Toast.makeText(this, "Location required!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                checkPermissionsAndConnect();
            } else {
                Toast.makeText(this, "Bluetooth required!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
