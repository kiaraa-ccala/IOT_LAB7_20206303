package com.example.lab7_20206303;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText codeEditText;
    private EditText pinEditText;
    private LinearLayout loadingLayout;
    private Button registerButton;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        authService = new AuthService(BackendClient.DEFAULT_BASE_URL);

        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        codeEditText = findViewById(R.id.codeEditText);
        pinEditText = findViewById(R.id.pinEditText);
        loadingLayout = findViewById(R.id.registerLoadingLayout);
        registerButton = findViewById(R.id.registerButton);

        findViewById(R.id.backTextView).setOnClickListener(v -> finish());
        registerButton.setOnClickListener(v -> register());
    }

    private void register() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String codigo = codeEditText.getText().toString().trim();
        String pin = pinEditText.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || codigo.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_LONG).show();
            return;
        }
        if (codigo.length() != 8 || pin.length() != 4) {
            Toast.makeText(this, "Codigo debe tener 8 digitos y PIN 4 digitos", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        authService.registerAfterBackendApproval(fullName, email, password, codigo, pin, new AuthService.RegisterCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        loadingLayout.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
        registerButton.setBackgroundResource(loading ? R.drawable.bg_disabled_button : R.drawable.bg_primary_button);
    }
}
