package com.bicipucp.validador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ValidadorController {
    @GetMapping("/validar/alumno/{codigo}")
    public boolean validarAlumno(@PathVariable String codigo) {
        return codigo != null && codigo.matches("20\\d{6}");
    }

    @GetMapping("/validar/candado/{pin}")
    public boolean validarCandado(@PathVariable String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            return false;
        }
        for (int i = 0; i < pin.length() - 1; i++) {
            if (pin.charAt(i) == pin.charAt(i + 1)) {
                return false;
            }
        }
        return true;
    }
}
