package com.udea.agora_backend.controller;

import com.udea.agora_backend.service.OAuthOnboardingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/estudiante")
    public ResponseEntity<Map<String, Object>> completarEstudiante(
            Authentication authentication,
            @Valid @RequestBody EstudianteRequest request) {
        Integer id = onboardingService.completarEstudiante(
                authentication.getName(), request.getIdPrograma(), request.getSemestre(),
                request.getIdLineasInvestigacion());
        return ResponseEntity.ok(Map.of("idEstudiante", id, "rol", "Estudiante"));
    }

    @PostMapping("/profesor")
    public ResponseEntity<Map<String, Object>> completarProfesor(
            Authentication authentication,
            @Valid @RequestBody ProfesorRequest request) {
        Integer id = onboardingService.completarProfesor(
                authentication.getName(), request.getIdPrograma(), request.getIdAreasEspecialidad());
        return ResponseEntity.ok(Map.of("idProfesor", id, "rol", "Profesor"));
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