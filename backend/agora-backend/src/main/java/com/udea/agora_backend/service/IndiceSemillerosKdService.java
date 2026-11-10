package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.CategoriasEstado;
import com.udea.agora_backend.dominio.EstadosSemillero;
import com.udea.agora_backend.estructuras.KdTree;
import com.udea.agora_backend.evento.SemillerosModificadosEvent;
import com.udea.agora_backend.model.AreaEspecialidad;
import com.udea.agora_backend.repository.AreaEspecialidadRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository.AreaDeSemillero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Índice espacial de los semilleros activos para el servicio "Semilleros similares a los tuyos".
 *
 * <p>Cada semillero se representa como un punto en un espacio de k dimensiones, una por cada
 * área de especialidad del catálogo. La coordenada i es cuántas de sus líneas de investigación
 * pertenecen al área i, y el vector se normaliza a longitud 1 (norma L2). Con vectores
 * unitarios, la distancia euclidiana d y la similitud coseno están ligadas por
 * {@code cos = 1 - d²/2}: el vecino más cercano en el árbol es el semillero de temática
 * más parecida.
 *
 * <p>Se usan áreas y no líneas porque el árbol KD solo poda bien con pocas dimensiones:
 * hay pocas áreas, mientras que una dimensión por línea daría decenas.
 *
 * <p>El árbol se guarda en memoria y se reutiliza entre consultas. Se descarta cuando se
 * publica {@link SemillerosModificadosEvent} (cambios hechos desde la aplicación) y, como
 * respaldo para los cambios hechos directamente en la base de datos, cuando pasa la vigencia
 * configurada en {@code app.recomendacion.kd.vigencia-minutos}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndiceSemillerosKdService {

    private final AreaEspecialidadRepository areaRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;

    @Value("${app.recomendacion.kd.vigencia-minutos:15}")
    private long vigenciaMinutos;

    // Índice vigente; null cuando debe reconstruirse. Es inmutable, así que varias consultas
    // concurrentes pueden leerlo sin bloquearse.
    private volatile Indice indice;

    /** Dimensión del espacio: un área de especialidad. */
    public record Dimension(Integer idArea, String nombreArea) {
    }

    /**
     * Fotografía del índice en un momento dado.
     *
     * @param arbol       árbol KD con un punto por semillero activo que tiene líneas
     * @param dimensiones áreas de especialidad, en el orden de las coordenadas
     * @param construidoEn momento de construcción, para la vigencia
     */
    public record Indice(KdTree<Integer> arbol, List<Dimension> dimensiones,
                         Map<Integer, Integer> posicionPorArea, Instant construidoEn) {

        /**
         * Convierte una colección de áreas (con repeticiones: una por línea) en un vector
         * unitario del espacio del índice. Devuelve null si ninguna área cae en el espacio.
         */
        public double[] vectorizar(Collection<Integer> idsArea) {
            return IndiceSemillerosKdService.vectorizar(idsArea, posicionPorArea, arbol.dimensiones());
        }
    }

    /** Devuelve el índice vigente, construyéndolo si no existe o ya venció. */
    @Transactional(readOnly = true)
    public Indice obtener() {
        Indice actual = indice;
        if (actual != null && !estaVencido(actual)) {
            return actual;
        }
        synchronized (this) {
            // Otra petición pudo reconstruirlo mientras esta esperaba el bloqueo
            if (indice == null || estaVencido(indice)) {
                indice = construir();
            }
            return indice;
        }
    }

    /** Se ejecuta después del commit de la transacción que publicó el evento. */
    @TransactionalEventListener(fallbackExecution = true)
    public void alModificarSemilleros(SemillerosModificadosEvent evento) {
        indice = null;
        log.debug("Árbol KD de semilleros invalidado; se reconstruirá en la próxima consulta");
    }

    private boolean estaVencido(Indice actual) {
        return actual.construidoEn().plus(Duration.ofMinutes(vigenciaMinutos)).isBefore(Instant.now());
    }

    private Indice construir() {
        List<Dimension> dimensiones = areaRepository.findAll().stream()
                .sorted(Comparator.comparing(AreaEspecialidad::getId))
                .map(area -> new Dimension(area.getId(), area.getNombre()))
                .toList();
        Map<Integer, Integer> posicionPorArea = new HashMap<>();
        for (int i = 0; i < dimensiones.size(); i++) {
            posicionPorArea.put(dimensiones.get(i).idArea(), i);
        }

        // Una sola consulta: (semillero, área) por cada línea de cada semillero activo
        Map<Integer, List<Integer>> areasPorSemillero = agruparPorSemillero(
                semilleroLineaRepository.findAreasDeSemillerosPorEstado(
                        EstadosSemillero.ACTIVO, CategoriasEstado.SEMILLERO));

        // Sin áreas en el catálogo el árbol queda vacío (con una dimensión nominal)
        int k = Math.max(1, dimensiones.size());
        List<KdTree.Entrada<Integer>> entradas = new ArrayList<>();
        areasPorSemillero.forEach((idSemillero, areas) -> {
            // Un semillero sin líneas (o con áreas fuera del catálogo) no tiene posición: queda fuera
            double[] vector = vectorizar(areas, posicionPorArea, k);
            if (vector != null) {
                entradas.add(new KdTree.Entrada<>(vector, idSemillero));
            }
        });

        KdTree<Integer> arbol = KdTree.construir(k, entradas);
        log.info("Árbol KD de semilleros construido: {} semilleros, {} dimensiones, profundidad {}",
                arbol.tamano(), arbol.dimensiones(), arbol.profundidad());
        return new Indice(arbol, dimensiones, posicionPorArea, Instant.now());
    }

    /** Agrupa los pares (semillero, área) por semillero, conservando las repeticiones. */
    public static Map<Integer, List<Integer>> agruparPorSemillero(List<AreaDeSemillero> filas) {
        return filas.stream().collect(Collectors.groupingBy(
                AreaDeSemillero::getIdSemillero, LinkedHashMap::new,
                Collectors.mapping(AreaDeSemillero::getIdArea, Collectors.toList())));
    }

    /**
     * Cuenta cuántas veces aparece cada área (una por línea de investigación) y normaliza el
     * resultado. Las áreas que no están en el espacio del índice se ignoran.
     */
    private static double[] vectorizar(Collection<Integer> idsArea, Map<Integer, Integer> posicionPorArea,
                                       int dimensiones) {
        double[] vector = new double[dimensiones];
        for (Integer idArea : idsArea) {
            Integer posicion = posicionPorArea.get(idArea);
            if (posicion != null) {
                vector[posicion]++;
            }
        }
        return normalizar(vector);
    }

    /** Escala el vector a longitud 1. Devuelve null para el vector cero (no tiene dirección). */
    public static double[] normalizar(double[] vector) {
        double norma = Math.sqrt(productoPunto(vector, vector));
        if (norma == 0) {
            return null;
        }
        double[] unitario = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            unitario[i] = vector[i] / norma;
        }
        return unitario;
    }

    public static double productoPunto(double[] a, double[] b) {
        double suma = 0;
        for (int i = 0; i < a.length; i++) {
            suma += a[i] * b[i];
        }
        return suma;
    }
}
