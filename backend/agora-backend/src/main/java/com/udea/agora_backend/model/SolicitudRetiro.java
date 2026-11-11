package com.udea.agora_backend.model;

import com.udea.agora_backend.dominio.EstadosSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Solicitud de un integrante para salir de un semillero (migración V4). La responde un líder
 * (estudiante líder o tutor); si la aprueba, el integrante queda retirado.
 */
@Entity
@Table(name = "tbl_solicitudes_retiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudRetiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante_semillero", nullable = false)
    private EstudianteSemillero vinculacion;

    @Column(nullable = false)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private ZonedDateTime fechaSolicitud;

    @Column(name = "fecha_respuesta")
    private ZonedDateTime fechaRespuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_responde")
    private Usuario usuarioResponde;

    @Column(name = "comentario_respuesta")
    private String comentarioRespuesta;

    public boolean estaPendiente() {
        return estado != null && estado.tieneNombre(EstadosSolicitud.PENDIENTE);
    }
}
