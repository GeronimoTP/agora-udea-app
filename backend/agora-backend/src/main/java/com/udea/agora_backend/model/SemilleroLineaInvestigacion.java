package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "tbl_semillero_x_linea_investigacion",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_semillero", "id_linea_investigacion"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemilleroLineaInvestigacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private Semillero semillero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_linea_investigacion", nullable = false)
    private LineaInvestigacion lineaInvestigacion;
}
