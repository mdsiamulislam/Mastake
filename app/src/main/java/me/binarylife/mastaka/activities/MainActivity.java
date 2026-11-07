package me.binarylife.mastaka.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import androidx.drawerlayout.widget.DrawerLayout;

import me.binarylife.mastaka.fragments.NotesFragment;
import me.binarylife.mastaka.repository.NoteRepository;
import me.binarylife.mastaka.R;
import me.binarylife.mastaka.utils.IoTBackgroundSyncHelper;
import me.binarylife.mastaka.utils.ThemeManager;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //dynamic Status Bar Color
        getWindow().setStatusBarColor(getResources().getColor(R.color.secondaryContainer, getTheme()));

        // Initialize Views
        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddNote = findViewById(R.id.fabAddNote);

        // Setup Toolbar
        setSupportActionBar(toolbar);

        // Auto sync data to IoT device on app start (background)
        IoTBackgroundSyncHelper.syncToIoT(this);

        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        // Initial Page
        if (savedInstanceState == null) {
            showPage("Personal Notes", false);

            navigationView.setCheckedItem(R.id.nav_personal_notes);
            bottomNavigation.setSelectedItemId(R.id.nav_personal_notes);
        }

        // Bottom Navigation
        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_personal_notes) {
                showPage("Personal Notes", false);
                navigationView.setCheckedItem(R.id.nav_personal_notes);
                return true;

            } else if (id == R.id.nav_ai_notes) {
                showPage("AI Assist Notes", true);
                navigationView.setCheckedItem(R.id.nav_ai_notes);
                return true;
            }

            return false;
        });

        // Navigation Drawer
        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_personal_notes) {

                bottomNavigation.setSelectedItemId(R.id.nav_personal_notes);

            } else if (id == R.id.nav_ai_notes) {

                bottomNavigation.setSelectedItemId(R.id.nav_ai_notes);

            } else if (id == R.id.nav_iot) {

                startActivity(new Intent(MainActivity.this, IotActivity.class));

            } else if (id == R.id.nav_theme) {

                showThemeDialog();

            } else if (id == R.id.nav_about) {

                // Show About Dialog
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("About Mastaka")
                        .setMessage("Mastaka is a note-taking app with AI assistance and IoT integration.\n\nVersion: 1.0.0\n\nDeveloped by BinaryLife.")
                        .setPositiveButton("OK", null)
                        .show();

            } else if (id == R.id.nav_logout) {

                // Logout logic here with Firebase Authentication
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            drawerLayout.closeDrawer(GravityCompat.START);

            return true;
        });

        // Floating Action Button
        fabAddNote.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, NoteCreateActivity.class);
            startActivity(intent);

        });
    }

    private void showThemeDialog() {
        String[] themes = new String[]{"Light", "Dark", "System default"};
        int currentMode = ThemeManager.getSavedMode(this);
        int selectedIndex = currentMode;
        if (selectedIndex < 0 || selectedIndex > 2) selectedIndex = 2;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                    ThemeManager.saveMode(this, which);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Toolbar Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                NoteRepository repository = new NoteRepository(this);
                repository.fetchAndSyncFromCloud(currentUser.getUid());
                repository.triggerSync();
                Toast.makeText(this, "Refreshing notes from cloud...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Fragment Navigation
    private void showPage(String title, boolean isAiAssist) {

        toolbar.setTitle(title);

        Fragment fragment = NotesFragment.newInstance(isAiAssist);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
