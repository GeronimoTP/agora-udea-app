package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Requisito de texto que el estudiante declaró cumplir al postularse (migración V4). Los
 * requisitos de habilidad no se guardan aquí: se comparan con las habilidades de la postulación.
 */
@Entity
@Table(
    name = "tbl_postulacion_x_requisito",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_postulacion", "id_requisito"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostulacionRequisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulacion", nullable = false)
    private Postulacion postulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requisito", nullable = false)
    private RequisitoConvocatoria requisito;
}
