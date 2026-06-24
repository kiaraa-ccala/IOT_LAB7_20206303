package com.bicipucp.orquestador.dto;

public class ErrorResponse {
    private String mensaje;

    public ErrorResponse(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
