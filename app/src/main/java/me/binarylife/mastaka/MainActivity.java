
package me.binarylife.mastaka;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;

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

        // Initialize Views
        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddNote = findViewById(R.id.fabAddNote);

        // Setup Toolbar
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        // Initial Page
        if (savedInstanceState == null) {
            showPage("My Notes");

            navigationView.setCheckedItem(R.id.nav_notes);
            bottomNavigation.setSelectedItemId(R.id.nav_notes);
        }

        // Bottom Navigation
        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_notes) {
                showPage("My Notes");
                navigationView.setCheckedItem(R.id.nav_notes);
                return true;

            } else if (id == R.id.nav_settings) {
                showPage("Settings");
                navigationView.setCheckedItem(R.id.nav_settings);
                return true;
            }

            return false;
        });

        // Navigation Drawer
        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_notes) {

                bottomNavigation.setSelectedItemId(R.id.nav_notes);

            } else if (id == R.id.nav_settings) {

                bottomNavigation.setSelectedItemId(R.id.nav_settings);

            } else if (id == R.id.nav_iot) {

                Toast.makeText(this,
                        "IoT Devices coming soon!",
                        Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_about) {

                Toast.makeText(this,
                        "Mastake - Smart Note Taking App",
                        Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_logout) {

                Toast.makeText(this,
                        "Logout coming soon!",
                        Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);

            return true;
        });

        // Floating Action Button
        fabAddNote.setOnClickListener(v -> {

            Toast.makeText(this,
                    "Create New Note",
                    Toast.LENGTH_SHORT).show();

        });
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

        if (id == R.id.action_search) {

            Toast.makeText(this,
                    "Search Notes",
                    Toast.LENGTH_SHORT).show();

            return true;

        } else if (id == R.id.action_iot) {

            Toast.makeText(this,
                    "IoT Devices",
                    Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Fragment Navigation
    private void showPage(String title) {

        toolbar.setTitle(title);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer,
                        PageFragment.newInstance(title))
                .commit();
    }

    // Temporary Page Fragment
    public static class PageFragment extends Fragment {

        public static PageFragment newInstance(String title) {

            PageFragment fragment = new PageFragment();

            Bundle args = new Bundle();
            args.putString("title", title);

            fragment.setArguments(args);

            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(
                @NonNull android.view.LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {

            TextView textView = new TextView(requireContext());

            String title = getArguments() != null
                    ? getArguments().getString("title")
                    : "Mastake";

            textView.setText(title);
            textView.setTextSize(22);
            textView.setTextColor(0xFF172554);
            textView.setGravity(Gravity.CENTER);

            return textView;
        }
    }
}
