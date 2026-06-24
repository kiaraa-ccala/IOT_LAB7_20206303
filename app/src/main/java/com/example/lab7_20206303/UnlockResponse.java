package com.example.lab7_20206303;

import org.json.JSONException;
import org.json.JSONObject;

public class UnlockResponse {
    public final String status;
    public final String iotAuthToken;
    public final int desbloqueoExpiraEn;
    public final String timestampAprobacion;

    public UnlockResponse(String status, String iotAuthToken, int desbloqueoExpiraEn, String timestampAprobacion) {
        this.status = status;
        this.iotAuthToken = iotAuthToken;
        this.desbloqueoExpiraEn = desbloqueoExpiraEn;
        this.timestampAprobacion = timestampAprobacion;
    }

    public static UnlockResponse fromJson(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        return new UnlockResponse(
                object.optString("status"),
                object.optString("iot_auth_token"),
                object.optInt("desbloqueo_expira_en", 120),
                object.optString("timestamp_aprobacion")
        );
    }
}
