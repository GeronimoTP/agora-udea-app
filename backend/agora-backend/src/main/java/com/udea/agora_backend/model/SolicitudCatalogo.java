package com.udea.agora_backend.model;

import com.udea.agora_backend.dominio.EstadosSolicitud;
import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Solicitud de un estudiante o profesor para agregar un elemento al catálogo (migración V4).
 * El administrador la aprueba (y el elemento se crea) o la rechaza.
 */
@Entity
@Table(name = "tbl_solicitudes_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoElementoCatalogo tipo;

    @Column(nullable = false)
    private String nombre;

    // Obligatoria para todo tipo excepto habilidades
    @Column
    private String descripcion;

    // Solo para líneas de investigación
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area_especialidad")
    private AreaEspecialidad areaEspecialidad;

    @Column(nullable = false)
    private String justificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_solicitante", nullable = false)
    private Usuario solicitante;

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

    @Column(name = "id_elemento_creado")
    private Integer idElementoCreado;

    public boolean estaPendiente() {
        return estado != null && estado.tieneNombre(EstadosSolicitud.PENDIENTE);
    }
}
