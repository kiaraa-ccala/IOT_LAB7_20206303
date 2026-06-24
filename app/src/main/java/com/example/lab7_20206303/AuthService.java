package com.example.lab7_20206303;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthService {
    public interface RegisterCallback {
        void onSuccess();
        void onError(String message);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final BackendClient backendClient;

    public AuthService(String baseUrl) {
        backendClient = new BackendClient(baseUrl);
    }

    public void registerAfterBackendApproval(String fullName, String email, String password, String codigo, String pin, RegisterCallback callback) {
        backendClient.solicitarDesbloqueo(codigo, pin, new UnlockCallback() {
            @Override
            public void onSuccess(UnlockResponse response) {
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(result -> {
                            String uid = result.getUser().getUid();
                            Map<String, Object> profile = buildProfile(uid, fullName, email, codigo, response);
                            firestore.collection("usuarios").document(uid).set(profile)
                                    .addOnSuccessListener(unused -> callback.onSuccess())
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private Map<String, Object> buildProfile(String uid, String fullName, String email, String codigo, UnlockResponse response) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", uid);
        profile.put("nombre_completo", fullName);
        profile.put("correo", email);
        profile.put("codigo", codigo);
        profile.put("status", response.status);
        profile.put("iot_auth_token", response.iotAuthToken);
        profile.put("desbloqueo_expira_en", response.desbloqueoExpiraEn);
        profile.put("timestamp_aprobacion", response.timestampAprobacion);
        return profile;
    }
}
