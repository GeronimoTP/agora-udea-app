package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CandidatoPriorizadoDTO {
    private Integer idPostulacion;
    private Integer idConvocatoria;
    private String tituloConvocatoria;
    // Posición dentro de su convocatoria (1 = mejor candidato)
    private Integer posicion;
    private Integer cuposDisponibles;
    private Long ofertasVigentes;
    private LocalDate fechaPostulacion;
    private Double puntaje;
    private Double afinidadTematica;
    private Double habilidades;
    private Double disponibilidad;
    private Double compromiso;
    private List<String> observaciones;
    // Requisitos obligatorios de la convocatoria: quien no los cumple va después de los que sí
    private boolean cumpleObligatorios;
    private List<String> requisitosObligatoriosIncumplidos;
}
