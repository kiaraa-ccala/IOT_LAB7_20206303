package com.example.lab7_20206303;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CarnetActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private ImageView photoImageView;
    private TextView fullNameTextView;
    private TextView codeTextView;
    private TextView photoUrlTextView;
    private Button uploadPhotoButton;
    private ActivityResultLauncher<String> imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carnet);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        photoImageView = findViewById(R.id.photoImageView);
        fullNameTextView = findViewById(R.id.fullNameTextView);
        codeTextView = findViewById(R.id.codeTextView);
        photoUrlTextView = findViewById(R.id.photoUrlTextView);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);

        findViewById(R.id.backTextView).setOnClickListener(v -> finish());
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadCompressedPhoto(uri);
            }
        });
        uploadPhotoButton.setOnClickListener(v -> imagePicker.launch("image/*"));
        loadProfile();
    }

    private void loadProfile() {
        String uid = auth.getCurrentUser().getUid();
        firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    fullNameTextView.setText(valueOrDash(snapshot.getString("nombre_completo")));
                    codeTextView.setText(valueOrDash(snapshot.getString("codigo")));
                    String fotoUrl = snapshot.getString("foto_url");
                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        photoUrlTextView.setText(fotoUrl);
                        Glide.with(this).load(fotoUrl).centerCrop().into(photoImageView);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String valueOrDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private void uploadCompressedPhoto(Uri uri) {
        try {
            byte[] jpeg = compressToJpeg(uri);
            String uid = auth.getCurrentUser().getUid();
            StorageReference reference = storage.getReference().child("credenciales_bicipucp/" + uid + ".jpg");
            uploadPhotoButton.setEnabled(false);
            reference.putBytes(jpeg)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }
                        return reference.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> savePhotoUrl(downloadUri.toString()))
                    .addOnFailureListener(e -> {
                        uploadPhotoButton.setEnabled(true);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] compressToJpeg(Uri uri) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min(1f, 900f / Math.max(width, height));
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);
            return outputStream.toByteArray();
        }
    }

    private void savePhotoUrl(String url) {
        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("foto_url", url);
        firestore.collection("usuarios").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    uploadPhotoButton.setEnabled(true);
                    photoUrlTextView.setText(url);
                    Glide.with(this).load(url).centerCrop().into(photoImageView);
                    Toast.makeText(this, url, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    uploadPhotoButton.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
