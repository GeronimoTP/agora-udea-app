package com.udea.agora_backend.model;

import com.udea.agora_backend.dominio.EstadosConvocatoria;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "tbl_convocatorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convocatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "cupos_totales", nullable = false)
    private Integer cuposTotales;

    @Column(name = "cupos_disponibles", nullable = false)
    private Integer cuposDisponibles;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private Semillero semillero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public boolean estaAbierta() {
        return estado != null && estado.tieneNombre(EstadosConvocatoria.ABIERTA);
    }

    /** Abierta y con fecha de cierre no vencida. */
    public boolean estaVigente(LocalDate hoy) {
        return estaAbierta() && !fechaCierre.isBefore(hoy);
    }

    public boolean tieneCuposDisponibles() {
        return cuposDisponibles != null && cuposDisponibles > 0;
    }

    /** Un estudiante puede postularse hoy: vigente, ya iniciada y con cupos. */
    public boolean recibePostulaciones(LocalDate hoy) {
        return estaVigente(hoy) && !fechaInicio.isAfter(hoy) && tieneCuposDisponibles();
    }
}
