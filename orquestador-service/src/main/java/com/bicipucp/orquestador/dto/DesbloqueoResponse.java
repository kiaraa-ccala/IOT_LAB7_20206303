package com.bicipucp.orquestador.dto;

public class DesbloqueoResponse {
    private String status;
    private String iot_auth_token;
    private int desbloqueo_expira_en;
    private String timestamp_aprobacion;

    public DesbloqueoResponse(String status, String iotAuthToken, int desbloqueoExpiraEn, String timestampAprobacion) {
        this.status = status;
        this.iot_auth_token = iotAuthToken;
        this.desbloqueo_expira_en = desbloqueoExpiraEn;
        this.timestamp_aprobacion = timestampAprobacion;
    }

    public String getStatus() {
        return status;
    }

    public String getIot_auth_token() {
        return iot_auth_token;
    }

    public int getDesbloqueo_expira_en() {
        return desbloqueo_expira_en;
    }

    public String getTimestamp_aprobacion() {
        return timestamp_aprobacion;
    }
}
