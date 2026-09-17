package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.EstudianteRequestDTO;
import com.udea.agora_backend.dto.request.ProfesorRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.exception.RecursoYaExisteException;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Profesor;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import com.udea.agora_backend.service.EstudianteService;
import com.udea.agora_backend.service.ProfesorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador de Autenticación
 * 
 * Maneja:
 * - Verificación de usuarios existentes
 * - Registro de nuevos estudiantes
 * - Registro de nuevos profesores
 * - Obtención de usuario actual autenticado
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final EstudianteService estudianteService;
    private final ProfesorService profesorService;

    /**
     * Verifica si un usuario ya existe en el sistema
     * 
     * GET /api/auth/check-email?email=usuario@udea.edu.co
     * 
     * @param email Email del usuario
     * @return { exists: boolean, id?: number, rol?: string }
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

        Map<String, Object> response = new HashMap<>();
        if (usuario.isPresent()) {
            response.put("exists", true);
            response.put("id", usuario.get().getId());
            response.put("rol", usuario.get().getRol().getNombre());
        } else {
            response.put("exists", false);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Registra un nuevo estudiante
     * 
     * POST /api/auth/register/estudiante
     * 
     * Body:
     * {
     *   "email": "estudiante@udea.edu.co",
     *   "nombreCompleto": "Juan Pérez",
     *   "idPrograma": 1,
     *   "semestre": 5,
     *   "idLineasInvestigacion": [1, 2, 3]
     * }
     */
    @PostMapping("/register/estudiante")
    public ResponseEntity<Map<String, Object>> registrarEstudiante(
            @Valid @RequestBody RegistroEstudianteDTO request) {

        // Verificar si el usuario ya existe
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RecursoYaExisteException("Usuario con este email ya existe");
        }

        try {
            // Crear usuario base y estudiante
            Estudiante estudiante = estudianteService.crear(
                    request.getEmail(),
                    request.getNombreCompleto(),
                    request.getIdPrograma(),
                    request.getSemestre(),
                    request.getIdLineasInvestigacion()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estudiante registrado exitosamente");
            response.put("idEstudiante", estudiante.getId());
            response.put("email", estudiante.getUsuario().getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RecursoNoEncontradoException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al registrar estudiante: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Registra un nuevo profesor
     * 
     * POST /api/auth/register/profesor
     * 
     * Body:
     * {
     *   "email": "profesor@udea.edu.co",
     *   "nombreCompleto": "Dra. María López",
     *   "idPrograma": 1,
     *   "idAreasEspecialidad": [1, 2]
     * }
     */
    @PostMapping("/register/profesor")
    public ResponseEntity<Map<String, Object>> registrarProfesor(
            @Valid @RequestBody RegistroProfesorDTO request) {

        // Verificar si el usuario ya existe
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RecursoYaExisteException("Usuario con este email ya existe");
        }

        try {
            // Crear usuario base y profesor
            Profesor profesor = profesorService.crear(
                    request.getEmail(),
                    request.getNombreCompleto(),
                    request.getIdPrograma(),
                    request.getIdAreasEspecialidad()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profesor registrado exitosamente");
            response.put("idProfesor", profesor.getId());
            response.put("email", profesor.getUsuario().getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RecursoNoEncontradoException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al registrar profesor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * DTOs para registro
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RegistroEstudianteDTO {
        private String email;
        private String nombreCompleto;
        private Integer idPrograma;
        private Integer semestre;
        private java.util.List<Integer> idLineasInvestigacion;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RegistroProfesorDTO {
        private String email;
        private String nombreCompleto;
        private Integer idPrograma;
        private java.util.List<Integer> idAreasEspecialidad;
    }
}
