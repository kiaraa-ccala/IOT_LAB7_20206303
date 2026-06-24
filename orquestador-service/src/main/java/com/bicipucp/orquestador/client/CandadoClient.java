package com.bicipucp.orquestador.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pucp-validador-service")
public interface CandadoClient {
    @GetMapping("/validar/candado/{pin}")
    Boolean validarCandado(@PathVariable("pin") String pin);
}
