package com.udea.agora_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de "Semilleros similares a los tuyos": semilleros más cercanos en el árbol KD
 * al punto que resume el historial (o el perfil) del estudiante.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemillerosSimilaresResponseDTO {

    /** Con qué se construyó el punto de consulta. */
    public enum Base {
        /** Semilleros en los que el estudiante está, estuvo o lidera. */
        HISTORIAL,
        /** Líneas de investigación de su perfil (aún no tiene historial con líneas). */
        PERFIL,
        /** Ni historial ni líneas de interés: no hay con qué comparar. */
        SIN_DATOS
    }

    private Base base;
    private List<SemilleroReferencia> referencias;
    private List<SemilleroSimilar> similares;
    private BusquedaKd busqueda;

    /** Semillero del historial del estudiante usado para construir la consulta. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemilleroReferencia {
        private Integer idSemillero;
        private String nombre;
        /** true si sigue siendo integrante (o lo lidera); false si ya salió. */
        private boolean actual;
        private double peso;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemilleroSimilar {
        private Integer idSemillero;
        private String nombre;
        private String codigoIdentificador;
        private String nombrePrograma;
        /** Similitud coseno entre el semillero y la consulta, de 0 a 100. */
        private double porcentajeSimilitud;
        /** Distancia euclidiana en el árbol (0 = idéntico, √2 = nada en común). */
        private double distancia;
        private List<String> areasEnComun;
        /** Líneas de investigación que comparte con el historial o el perfil: desempatan. */
        private List<String> lineasEnComun;
        /** Semillero del historial al que más se parece; null si la base es el perfil. */
        private String parecidoA;
        private boolean convocatoriaAbierta;
    }

    /** Datos de la búsqueda, para observar el comportamiento del árbol. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusquedaKd {
        private int dimensiones;
        private List<String> nombresDimensiones;
        private int semillerosEnArbol;
        private int profundidadArbol;
        private int nodosVisitados;
        /** Vecinos pedidos al árbol antes del desempate por líneas y convocatoria. */
        private int candidatosEvaluados;
    }
}
