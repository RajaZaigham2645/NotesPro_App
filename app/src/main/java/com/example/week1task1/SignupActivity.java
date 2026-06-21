package com.example.week1task1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class SignupActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilEmail, tilPassword, tilConfirmPassword;
    private Button btnSignup;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme preference
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.til_signup_email);
        tilPassword = findViewById(R.id.til_signup_password);
        tilConfirmPassword = findViewById(R.id.til_signup_confirm_password);

        etEmail = findViewById(R.id.et_signup_email);
        etPassword = findViewById(R.id.et_signup_password);
        etConfirmPassword = findViewById(R.id.et_signup_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);

        btnSignup.setOnClickListener(v -> registerUser());
        findViewById(R.id.tv_go_to_login).setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return;
        }

        btnSignup.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnSignup.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthWeakPasswordException) {
                            tilPassword.setError("The password is too weak.");
                        } else if (e instanceof FirebaseAuthUserCollisionException) {
                            tilEmail.setError("This email is already in use.");
                        } else {
                            Toast.makeText(SignupActivity.this, "Error: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
