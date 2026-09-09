package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "tbl_estudiantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relación OneToOne porque el id_usuario es 'unique' en DBML
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programa", nullable = false)
    private ProgramaAcademico programa;

    @Column(nullable = false)
    private Integer semestre;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
