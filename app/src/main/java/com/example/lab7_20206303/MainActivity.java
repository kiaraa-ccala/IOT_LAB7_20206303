package com.example.lab7_20206303;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private BackendClient backendClient;
    private LinearLayout statePanel;
    private TextView stateTitleTextView;
    private TextView countdownTextView;
    private TextView stateMessageTextView;
    private Button renewButton;
    private ProgressBar renewProgressBar;
    private CountDownTimer countDownTimer;
    private String codigoPucp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        firestore = FirebaseFirestore.getInstance();
        backendClient = new BackendClient(BackendClient.DEFAULT_BASE_URL);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("BiciPUCP");
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        statePanel = findViewById(R.id.statePanel);
        stateTitleTextView = findViewById(R.id.stateTitleTextView);
        countdownTextView = findViewById(R.id.countdownTextView);
        stateMessageTextView = findViewById(R.id.stateMessageTextView);
        renewButton = findViewById(R.id.renewButton);
        renewProgressBar = findViewById(R.id.renewProgressBar);
        renewButton.setOnClickListener(v -> askPinAndRenew());

        showLoadingState();
        loadProfileAndStartStateMachine();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }
        auth.getCurrentUser().reload()
                .addOnFailureListener(e -> {
                    auth.signOut();
                    navigateToLogin();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_carnet) {
            startActivity(new Intent(this, CarnetActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            auth.signOut();
            navigateToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadProfileAndStartStateMachine() {
        String uid = auth.getCurrentUser().getUid();
        firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    codigoPucp = snapshot.getString("codigo");
                    String timestamp = snapshot.getString("timestamp_aprobacion");
                    Long seconds = snapshot.getLong("desbloqueo_expira_en");
                    int graceSeconds = seconds == null ? 120 : seconds.intValue();
                    startStateMachine(timestamp, graceSeconds);
                })
                .addOnFailureListener(e -> {
                    showErrorState("No se pudo cargar el estado del candado");
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void startStateMachine(String timestampAprobacion, int graceSeconds) {
        long remaining = calculateRemainingSeconds(timestampAprobacion, graceSeconds);
        if (remaining > 0) {
            showActiveState(remaining);
        } else {
            showExpiredState();
        }
    }

    private long calculateRemainingSeconds(String timestampAprobacion, int graceSeconds) {
        LocalDateTime approvedAt = parseApprovalTime(timestampAprobacion);
        if (approvedAt == null) {
            return 0;
        }
        long elapsed = Duration.between(approvedAt, LocalDateTime.now()).getSeconds();
        if (elapsed < 0) {
            return graceSeconds;
        }
        return Math.max(0, graceSeconds - elapsed);
    }

    private LocalDateTime parseApprovalTime(String timestampAprobacion) {
        if (timestampAprobacion == null || timestampAprobacion.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(timestampAprobacion, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(timestampAprobacion)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(timestampAprobacion)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void showLoadingState() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        statePanel.setBackgroundResource(R.drawable.bg_active_state);
        stateTitleTextView.setText("CARGANDO ESTADO");
        stateTitleTextView.setTextColor(getColor(R.color.bicipucp_green));
        countdownTextView.setText("--");
        countdownTextView.setTextColor(getColor(R.color.bicipucp_green));
        stateMessageTextView.setText("Consultando datos de la credencial IoT");
        stateMessageTextView.setTextColor(getColor(R.color.bicipucp_green));
        renewButton.setVisibility(View.GONE);
        renewProgressBar.setVisibility(View.VISIBLE);
    }

    private void showActiveState(long remainingSeconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        renewProgressBar.setVisibility(View.GONE);
        renewButton.setVisibility(View.GONE);
        statePanel.setBackgroundResource(R.drawable.bg_active_state);
        stateTitleTextView.setText("ESTADO: EN GRACIA");
        stateTitleTextView.setTextColor(getColor(R.color.bicipucp_green));
        countdownTextView.setTextColor(getColor(R.color.bicipucp_green));
        stateMessageTextView.setTextColor(getColor(R.color.bicipucp_green));
        stateMessageTextView.setText("Candado IoT energizado - Retire la unidad");

        countDownTimer = new CountDownTimer(remainingSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1, millisUntilFinished / 1000L);
                countdownTextView.setText(seconds + "s");
            }

            @Override
            public void onFinish() {
                showExpiredState();
            }
        };
        countdownTextView.setText(remainingSeconds + "s");
        countDownTimer.start();
    }

    private void showExpiredState() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        statePanel.setBackgroundResource(R.drawable.bg_expired_state);
        stateTitleTextView.setText("ESTADO: EXPIRADO");
        stateTitleTextView.setTextColor(getColor(R.color.bicipucp_red));
        countdownTextView.setText("00s");
        countdownTextView.setTextColor(getColor(R.color.bicipucp_red));
        stateMessageTextView.setText("Tiempo de gracia expirado - Candado trabado por seguridad");
        stateMessageTextView.setTextColor(getColor(R.color.bicipucp_red));
        renewButton.setVisibility(View.VISIBLE);
        renewProgressBar.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        statePanel.setBackgroundResource(R.drawable.bg_expired_state);
        stateTitleTextView.setText("ESTADO NO DISPONIBLE");
        stateTitleTextView.setTextColor(getColor(R.color.bicipucp_red));
        countdownTextView.setText("--");
        countdownTextView.setTextColor(getColor(R.color.bicipucp_red));
        stateMessageTextView.setText(message);
        stateMessageTextView.setTextColor(getColor(R.color.bicipucp_red));
        renewButton.setVisibility(View.VISIBLE);
        renewProgressBar.setVisibility(View.GONE);
    }

    private void askPinAndRenew() {
        EditText input = new EditText(this);
        input.setHint("PIN Candado IoT");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setMaxLines(1);
        new AlertDialog.Builder(this)
                .setTitle("Nuevo desbloqueo")
                .setMessage("Ingrese nuevamente el PIN asignado")
                .setView(input)
                .setPositiveButton("Solicitar", (dialog, which) -> renew(input.getText().toString().trim()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void renew(String pin) {
        if (codigoPucp == null || codigoPucp.isEmpty()) {
            Toast.makeText(this, "No se encontro el codigo PUCP del perfil", Toast.LENGTH_LONG).show();
            return;
        }
        if (pin.length() != 4) {
            Toast.makeText(this, "PIN debe tener 4 digitos", Toast.LENGTH_LONG).show();
            return;
        }
        renewButton.setEnabled(false);
        renewProgressBar.setVisibility(View.VISIBLE);
        backendClient.solicitarDesbloqueo(codigoPucp, pin, new UnlockCallback() {
            @Override
            public void onSuccess(UnlockResponse response) {
                updateUnlockData(response);
            }

            @Override
            public void onError(String message) {
                renewButton.setEnabled(true);
                renewProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUnlockData(UnlockResponse response) {
        String uid = auth.getCurrentUser().getUid();
        String approvedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", response.status);
        updates.put("iot_auth_token", response.iotAuthToken);
        updates.put("desbloqueo_expira_en", response.desbloqueoExpiraEn);
        updates.put("timestamp_aprobacion", approvedAt);
        firestore.collection("usuarios").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    renewButton.setEnabled(true);
                    renewProgressBar.setVisibility(View.GONE);
                    showActiveState(response.desbloqueoExpiraEn);
                })
                .addOnFailureListener(e -> {
                    renewButton.setEnabled(true);
                    renewProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
