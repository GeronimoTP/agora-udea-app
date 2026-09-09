package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_oauth_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OauthProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String descripcion;
}
