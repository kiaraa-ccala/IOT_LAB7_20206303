package com.example.lab7_20206303;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendClient {
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String baseUrl;

    public BackendClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void solicitarDesbloqueo(String codigo, String pin, UnlockCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(baseUrl + "/bici/solicitar-desbloqueo");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);

                JSONObject request = new JSONObject();
                request.put("codigo", codigo);
                request.put("pin", pin);
                byte[] payload = request.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload);
                }

                int code = connection.getResponseCode();
                String body = readBody(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
                if (code == HttpURLConnection.HTTP_OK) {
                    UnlockResponse response = UnlockResponse.fromJson(body);
                    mainHandler.post(() -> callback.onSuccess(response));
                } else {
                    String message = extractErrorMessage(body);
                    mainHandler.post(() -> callback.onError(message));
                }
            } catch (Exception e) {
                String message = e.getMessage() == null ? "No se pudo conectar con Spring Boot" : e.getMessage();
                mainHandler.post(() -> callback.onError(message));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String readBody(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String extractErrorMessage(String body) {
        try {
            JSONObject object = new JSONObject(body);
            String message = object.optString("mensaje");
            return message.isEmpty() ? body : message;
        } catch (Exception ignored) {
            return body == null || body.isEmpty() ? "Solicitud rechazada por el servidor" : body;
        }
    }
}
