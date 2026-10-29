package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UsuarioSesionResponseDTO {
    private Integer idUsuario;
    private String nombreCompleto;
    private String email;
    private String rol;
    private Integer idEstudiante;
    private Integer idProfesor;
    private List<Integer> idSemillerosLiderados;
    // Semilleros en los que el estudiante es integrante activo (puede ser más de uno)
    private List<Integer> idSemillerosIntegrante;
}