package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_tipos_reconocimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoReconocimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String descripcion;
}
