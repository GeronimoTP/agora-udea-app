package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "tbl_reconocimientos_semilleros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconocimientoSemillero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private Semillero semillero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_reconocimiento", nullable = false)
    private TipoReconocimiento tipoReconocimiento;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entidad_otorgante", nullable = false)
    private EntidadOtorgante entidadOtorgante;

    @Column(name = "fecha_obtencion", nullable = false)
    private LocalDate fechaObtencion;

    @Column(name = "url_certificado", nullable = false)
    private String urlCertificado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
