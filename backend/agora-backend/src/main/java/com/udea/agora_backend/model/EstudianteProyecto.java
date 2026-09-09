package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "tbl_estudiante_x_proyecto",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_proyecto", "id_estudiante"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol_proyecto", nullable = false)
    private RolEstudianteProyecto rolProyecto;

    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDate fechaVinculacion;
}
