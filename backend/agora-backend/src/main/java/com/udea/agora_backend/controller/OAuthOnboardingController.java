package com.udea.agora_backend.controller;

import com.udea.agora_backend.service.OAuthOnboardingService;
import com.udea.agora_backend.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/onboarding")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OAuthOnboardingController {

    private final OAuthOnboardingService onboardingService;
    private final JwtService jwtService;

    @PostMapping("/estudiante")
    public ResponseEntity<Map<String, Object>> completarEstudiante(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody EstudianteRequest request) {
        String token = obtenerTokenOnboarding(authorization);
        Integer id = onboardingService.completarEstudiante(
            jwtService.extraerCorreo(token), jwtService.extraerOAuthId(token),
            jwtService.extraerNombreCompleto(token), request.getIdPrograma(), request.getSemestre(),
                request.getIdLineasInvestigacion());
        return ResponseEntity.ok(Map.of(
            "idEstudiante", id,
            "rol", "Estudiante",
            "accessToken", jwtService.generarToken(jwtService.extraerCorreo(token))));
    }

    @PostMapping("/profesor")
    public ResponseEntity<Map<String, Object>> completarProfesor(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ProfesorRequest request) {
        String token = obtenerTokenOnboarding(authorization);
        Integer id = onboardingService.completarProfesor(
                jwtService.extraerCorreo(token), jwtService.extraerOAuthId(token),
                jwtService.extraerNombreCompleto(token), request.getIdPrograma(), request.getIdAreasEspecialidad());
        return ResponseEntity.ok(Map.of(
                "idProfesor", id,
                "rol", "Profesor",
                "accessToken", jwtService.generarToken(jwtService.extraerCorreo(token))));
    }

    private String obtenerTokenOnboarding(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el token de onboarding");
        }
        String token = authorization.substring(7);
        if (!jwtService.esTokenOnboarding(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El token no permite completar el registro");
        }
        return token;
    }

    @Data
    public static class EstudianteRequest {
        @NotNull
        private Integer idPrograma;

        @NotNull
        @Min(1)
        private Integer semestre;

        @NotEmpty
        private List<Integer> idLineasInvestigacion;
    }

    @Data
    public static class ProfesorRequest {
        @NotNull
        private Integer idPrograma;

        @NotEmpty
        private List<Integer> idAreasEspecialidad;
    }
}