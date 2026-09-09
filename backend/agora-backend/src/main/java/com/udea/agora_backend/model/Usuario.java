package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "tbl_usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor_oauth", nullable = false)
    private OauthProveedor proveedorOauth;

    @Column(name = "oauth_id", nullable = false, unique = true)
    private String oauthId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    // Se usa ZonedDateTime para mapear 'timestamptz'
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
