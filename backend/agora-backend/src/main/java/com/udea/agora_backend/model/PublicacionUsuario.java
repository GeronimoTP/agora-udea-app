package com.udea.agora_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "tbl_publicacion_x_usuario",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_publicacion", "id_usuario"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol_autor", nullable = false)
    private RolAutor rolAutor;

    @Column(name = "fecha_asociacion", nullable = false)
    private LocalDate fechaAsociacion;
}
