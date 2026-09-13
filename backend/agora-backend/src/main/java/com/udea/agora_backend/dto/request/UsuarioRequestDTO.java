package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsuarioRequestDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo es inválido")
    @Pattern(
        regexp = "^[a-zA-Z0-9._%+-]+@udea\\.edu\\.co$", 
        message = "Debe registrarse con un correo institucional de la Universidad de Antioquia"
    )
    private String email;

    @NotNull(message = "El proveedor de OAuth es obligatorio")
    private Integer idProveedorOauth;

    @NotBlank(message = "El identificador de OAuth es obligatorio")
    private String oauthId;

    @NotNull(message = "El rol del usuario es obligatorio")
    private Integer idRol;
}
