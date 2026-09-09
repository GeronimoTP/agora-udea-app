package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "tbl_estudiante_x_semillero",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_semillero", "id_estudiante"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteSemillero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private Semillero semillero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    // Puede ser nulo si el estudiante sigue activo
    @Column(name = "fecha_salida")
    private LocalDate fechaSalida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;
}
