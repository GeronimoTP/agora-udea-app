package com.udea.agora_backend.dto.response;

import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class SolicitudCatalogoResponseDTO {
    private Integer id;
    private TipoElementoCatalogo tipo;
    private String etiquetaTipo;
    private String nombre;
    private String descripcion;
    private Integer idAreaEspecialidad;
    private String nombreAreaEspecialidad;
    private String justificacion;
    private Integer idSolicitante;
    private String nombreSolicitante;
    private String rolSolicitante;
    private String estado;
    private ZonedDateTime fechaSolicitud;
    private ZonedDateTime fechaRespuesta;
    private String nombreQuienResponde;
    private String comentarioRespuesta;
    // ID del elemento creado al aprobar, en el catálogo que indica "tipo"
    private Integer idElementoCreado;
}
