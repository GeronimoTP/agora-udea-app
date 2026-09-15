package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_linea_investigacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaInvestigacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String descripcion;

    // Cada línea de investigación pertenece a un área de especialidad más amplia.
    // Permite elevar el interés temático del estudiante (líneas) a nivel de área
    // para compararlo contra las especialidades de los profesores en el Factor 2
    // del motor de recomendación.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area_especialidad", nullable = false)
    private AreaEspecialidad areaEspecialidad;
}