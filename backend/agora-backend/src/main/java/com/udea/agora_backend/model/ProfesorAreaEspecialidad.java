package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "tbl_profesor_x_area_especialidad",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_profesor", "id_area_especialidad"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorAreaEspecialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profesor", nullable = false)
    private Profesor profesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area_especialidad", nullable = false)
    private AreaEspecialidad areaEspecialidad;
}
