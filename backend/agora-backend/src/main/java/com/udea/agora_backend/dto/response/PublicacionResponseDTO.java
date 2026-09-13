package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class PublicacionResponseDTO {
    private Integer id;
    private String titulo;
    private String urlDocumento;
    private LocalDate fechaPublicacion;
    private String nombreProyecto;
    private String nombreSemillero;
    private List<String> autores;
    private ZonedDateTime createdAt;
}
