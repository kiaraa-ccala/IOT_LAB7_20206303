package com.bicipucp.orquestador.controller;

import com.bicipucp.orquestador.client.CandadoClient;
import com.bicipucp.orquestador.dto.DesbloqueoResponse;
import com.bicipucp.orquestador.dto.ErrorResponse;
import com.bicipucp.orquestador.dto.SolicitudDesbloqueoRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/bici")
public class BiciController {
    private static final String VALIDADOR_BASE_URL = "http://localhost:8001";
    private final CandadoClient candadoClient;

    public BiciController(CandadoClient candadoClient) {
        this.candadoClient = candadoClient;
    }

    @PostMapping("/solicitar-desbloqueo")
    public ResponseEntity<?> solicitarDesbloqueo(@RequestBody SolicitudDesbloqueoRequest request) {
        if (request.getCodigo() == null || request.getPin() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Debe enviar codigo y PIN"));
        }

        RestTemplate restTemplate = new RestTemplate();
        Boolean alumnoValido = restTemplate.getForObject(
                VALIDADOR_BASE_URL + "/validar/alumno/{codigo}",
                Boolean.class,
                request.getCodigo()
        );
        Boolean candadoValido = candadoClient.validarCandado(request.getPin());

        if (!Boolean.TRUE.equals(alumnoValido)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El codigo de alumno no existe en la base de datos"));
        }
        if (!Boolean.TRUE.equals(candadoValido)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("El PIN del candado IoT no cumple las reglas de integridad"));
        }

        String token = "PUCP-BIKE-" + UUID.randomUUID().toString().substring(0, 8);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return ResponseEntity.ok(new DesbloqueoResponse("APROBADO", token, 120, timestamp));
    }
}
