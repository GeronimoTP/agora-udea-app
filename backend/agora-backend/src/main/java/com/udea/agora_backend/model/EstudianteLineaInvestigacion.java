package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "tbl_estudiante_x_linea_investigacion",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_estudiante", "id_linea_investigacion"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteLineaInvestigacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_linea_investigacion", nullable = false)
    private LineaInvestigacion lineaInvestigacion;
}
