package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_programas_academicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramaAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_facultad", nullable = false)
    private Facultad facultad;
}
