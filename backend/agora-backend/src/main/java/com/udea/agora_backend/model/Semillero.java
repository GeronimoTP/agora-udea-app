package com.udea.agora_backend.model;

import com.udea.agora_backend.dominio.EstadosSemillero;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "tbl_semilleros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semillero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo_identificador", nullable = false, unique = true)
    private String codigoIdentificador;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante_lider", nullable = false)
    private Estudiante estudianteLider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programa", nullable = false)
    private ProgramaAcademico programa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    // Revisión del administrador (migración V4). Son nulas en los semilleros anteriores a la
    // aprobación, que se consideran aprobados.
    @Column(name = "fecha_solicitud")
    private ZonedDateTime fechaSolicitud;

    @Column(name = "fecha_revision")
    private ZonedDateTime fechaRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revisor")
    private Usuario usuarioRevisor;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    public boolean estaActivo() {
        return estado != null && estado.tieneNombre(EstadosSemillero.ACTIVO);
    }

    public boolean estaPendienteDeAprobacion() {
        return estado != null && estado.tieneNombre(EstadosSemillero.PENDIENTE_APROBACION);
    }

    public boolean fueRechazado() {
        return estado != null && estado.tieneNombre(EstadosSemillero.RECHAZADO);
    }

    /**
     * Aprobado por el administrador (hoy activo o inactivo). Solo un semillero aprobado es
     * público y puede tener integrantes, proyectos y reconocimientos.
     */
    public boolean estaAprobado() {
        return !estaPendienteDeAprobacion() && !fueRechazado();
    }
}
