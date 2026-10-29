package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

// Habilidades que el estudiante declara en su perfil (migración V3)
@Entity
@Table(
    name = "tbl_estudiante_x_habilidad",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_estudiante", "id_habilidad"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteHabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habilidad", nullable = false)
    private Habilidad habilidad;
}
