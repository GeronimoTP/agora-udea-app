package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(
    name = "tbl_postulaciones",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_convocatoria", "id_estudiante"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_convocatoria", nullable = false)
    private Convocatoria convocatoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "respuesta_motivacion", nullable = false)
    private String respuestaMotivacion;

    @Column(name = "disponibilidad_horas_semana", nullable = false)
    private Integer disponibilidadHorasSemana;

    @Column(name = "experiencia_previa", nullable = false)
    private String experienciaPrevia;

    @Column(name = "fecha_postulacion", nullable = false)
    private LocalDate fechaPostulacion;

    // Momento en que el líder emitió la oferta (Pre-aprobada). Requiere la migración V2.
    @Column(name = "fecha_pre_aprobacion")
    private ZonedDateTime fechaPreAprobacion;

    @Column(name = "fecha_decision_estudiante")
    private ZonedDateTime fechaDecisionEstudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    void asignarFechasCreacion() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (fechaPostulacion == null) {
            fechaPostulacion = LocalDate.now();
        }
    }
}
