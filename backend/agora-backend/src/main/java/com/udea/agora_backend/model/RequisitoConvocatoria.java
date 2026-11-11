package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Requisito de una convocatoria (migración V4): una habilidad del catálogo o un texto libre,
 * obligatorio u opcional. Incumplir uno obligatorio no impide postularse, pero el líder lo ve
 * y la priorización ubica al aspirante después de los que sí cumplen.
 */
@Entity
@Table(name = "tbl_requisitos_convocatoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitoConvocatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_convocatoria", nullable = false)
    private Convocatoria convocatoria;

    // Exactamente uno de los dos: habilidad del catálogo o descripción libre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habilidad")
    private Habilidad habilidad;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(nullable = false)
    private boolean obligatorio;

    public boolean esDeHabilidad() {
        return habilidad != null;
    }

    /** Texto para mostrar: el nombre de la habilidad o la descripción libre. */
    public String texto() {
        return esDeHabilidad() ? habilidad.getNombre() : descripcion;
    }
}
