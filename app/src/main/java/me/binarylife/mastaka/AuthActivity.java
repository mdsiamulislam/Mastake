package me.binarylife.mastaka;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AuthActivity extends AppCompatActivity {

    LinearLayout btnGoogleSignIn;
    TextView googleIcon, googleText;
    ProgressBar googleProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        googleIcon = findViewById(R.id.googleIcon);
        googleText = findViewById(R.id.googleText);
        googleProgressBar = findViewById(R.id.googleProgressBar);

        btnGoogleSignIn.setOnClickListener(v -> {
            googleIcon.setVisibility(android.view.View.GONE);
            googleText.setVisibility(android.view.View.GONE);
            googleProgressBar.setVisibility(android.view.View.VISIBLE);

            startActivity(new android.content.Intent(AuthActivity.this, MainActivity.class));

        });
    }
}