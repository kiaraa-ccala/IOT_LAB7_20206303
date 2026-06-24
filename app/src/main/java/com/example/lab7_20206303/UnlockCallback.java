package com.example.lab7_20206303;

public interface UnlockCallback {
    void onSuccess(UnlockResponse response);
    void onError(String message);
}
