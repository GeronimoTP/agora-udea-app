package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "tbl_profesor_x_semillero",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_semillero", "id_profesor"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorSemillero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private Semillero semillero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profesor", nullable = false)
    private Profesor profesor;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;
}