package me.binarylife.mastaka.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import me.binarylife.mastaka.R;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";
    LinearLayout btnGoogleSignIn;
    TextView googleIcon, googleText;
    ProgressBar googleProgressBar;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        googleIcon = findViewById(R.id.googleIcon);
        googleText = findViewById(R.id.googleText);
        googleProgressBar = findViewById(R.id.googleProgressBar);

        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        googleIcon.setVisibility(View.GONE);
        googleText.setVisibility(View.GONE);
        googleProgressBar.setVisibility(View.VISIBLE);

        // Extracted Web Client ID from google-services.json (client_type 3)
        String webClientId = "111443439971-8n9gq08eu0e9q0jv55r5h00lmpi5it28.apps.googleusercontent.com";

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new android.os.CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        try {
                            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                            String googleIdToken = googleIdTokenCredential.getIdToken();
                            authenticateWithFirebase(googleIdToken);
                        } catch (Exception e) {
                            Log.e(TAG, "Invalid Google ID token credential", e);
                            resetUI();
                            Toast.makeText(AuthActivity.this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Get credential error", e);
                        resetUI();
                        Toast.makeText(AuthActivity.this, "Sign in failed: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void authenticateWithFirebase(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    resetUI();
                    if (task.isSuccessful()) {
                        Toast.makeText(AuthActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AuthActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(AuthActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetUI() {
        googleIcon.setVisibility(View.VISIBLE);
        googleText.setVisibility(View.VISIBLE);
        googleProgressBar.setVisibility(View.GONE);
    }
}
