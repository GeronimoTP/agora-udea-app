package com.udea.agora_backend.controller;

import com.udea.agora_backend.service.LoginPruebasService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/login-pruebas")
@RequiredArgsConstructor
public class LoginPruebasController {

    private final LoginPruebasService loginPruebasService;

    // El frontend solo muestra el formulario de pruebas si está habilitado
    @GetMapping
    public ResponseEntity<Map<String, Object>> estado() {
        return ResponseEntity.ok(Map.of("habilitado", loginPruebasService.estaHabilitado()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> iniciarSesion(@Valid @RequestBody LoginPruebasRequest request) {
        return ResponseEntity.ok(Map.of(
            "accessToken", loginPruebasService.iniciarSesion(request.getCorreo(), request.getClave())));
    }

    @Data
    public static class LoginPruebasRequest {
        @NotBlank
        private String correo;

        @NotBlank
        private String clave;
    }
}
