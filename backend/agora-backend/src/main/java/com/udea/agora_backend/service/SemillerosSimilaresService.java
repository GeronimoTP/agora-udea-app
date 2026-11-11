package com.udea.agora_backend.service;

import com.udea.agora_backend.config.PesosSemillerosSimilares;
import com.udea.agora_backend.dto.response.SemillerosSimilaresResponseDTO;
import com.udea.agora_backend.dto.response.SemillerosSimilaresResponseDTO.Base;
import com.udea.agora_backend.dto.response.SemillerosSimilaresResponseDTO.BusquedaKd;
import com.udea.agora_backend.dto.response.SemillerosSimilaresResponseDTO.SemilleroReferencia;
import com.udea.agora_backend.dto.response.SemillerosSimilaresResponseDTO.SemilleroSimilar;
import com.udea.agora_backend.estructuras.KdTree;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteLineaInvestigacionRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.service.IndiceSemillerosKdService.Indice;
import com.udea.agora_backend.util.Redondeo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SERVICIO 1 (complemento): "Semilleros similares a los tuyos".
 *
 * <p>Recomienda semilleros parecidos a aquellos en los que el estudiante está o ha estado,
 * con una búsqueda de vecinos más cercanos (k-NN) en el árbol KD de
 * {@link IndiceSemillerosKdService}:
 * <ol>
 *   <li>Cada semillero del historial se vectoriza por sus áreas de especialidad. Los actuales
 *       (integrante activo o líder) pesan más que los anteriores: 1 y 0,5 por defecto
 *       ({@link PesosSemillerosSimilares}).</li>
 *   <li>El punto de consulta es la suma ponderada de esos vectores, normalizada: el "centro
 *       de gravedad" temático del estudiante.</li>
 *   <li>Sin historial con líneas, el punto sale de las líneas de interés de su perfil.</li>
 *   <li>El árbol devuelve los {@value #CANDIDATOS_POR_RESULTADO}·k semilleros activos más
 *       cercanos, sin contar aquellos en los que el estudiante ya está, estuvo o lidera.</li>
 *   <li>Como hay pocas dimensiones, muchos semilleros empatan en distancia. Se ordenan por
 *       similitud y, a igual similitud, por líneas de investigación en común con el historial
 *       (o el perfil) y por convocatoria abierta. Se devuelven los k primeros.</li>
 * </ol>
 * Es independiente del ranking ponderado de {@link MotorRecomendacionService}: aquel puntúa
 * todos los semilleros con cuatro factores; este busca por similitud temática con el historial.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemillerosSimilaresService {

    public static final int LIMITE_MAXIMO = 10;
    // Vecinos que se piden al árbol por cada resultado, para poder desempatar
    static final int CANDIDATOS_POR_RESULTADO = 3;

    private final IndiceSemillerosKdService indiceService;
    private final EstudianteRepository estudianteRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final SemilleroRepository semilleroRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final PesosSemillerosSimilares pesos;

    public SemillerosSimilaresResponseDTO buscar(Integer idEstudiante, int limite) {
        if (!estudianteRepository.existsById(idEstudiante)) {
            throw new RecursoNoEncontradoException("Estudiante", idEstudiante);
        }
        int k = Math.clamp(limite, 1, LIMITE_MAXIMO);
        Indice indice = indiceService.obtener();

        // 1. Historial: idSemillero → peso. Liderar cuenta como pertenencia actual.
        Map<Integer, Double> pesoPorSemillero = new LinkedHashMap<>();
        for (EstudianteSemillero vinculacion : estudianteSemilleroRepository.findByEstudianteId(idEstudiante)) {
            double peso = vinculacion.estaActiva() ? pesos.actual() : pesos.anterior();
            pesoPorSemillero.merge(vinculacion.getSemillero().getId(), peso, Math::max);
        }
        for (Integer idLiderado : semilleroRepository.findIdsByEstudianteLiderId(idEstudiante)) {
            pesoPorSemillero.put(idLiderado, pesos.actual());
        }

        // 2. Vector de cada semillero del historial (pueden estar inactivos: se leen de la BD)
        Map<Integer, double[]> vectorPorSemillero = new LinkedHashMap<>();
        if (!pesoPorSemillero.isEmpty()) {
            IndiceSemillerosKdService.agruparPorSemillero(
                            semilleroLineaRepository.findAreasDeSemilleros(pesoPorSemillero.keySet()))
                    .forEach((idSemillero, areas) -> {
                        double[] vector = indice.vectorizar(areas);
                        if (vector != null) {
                            vectorPorSemillero.put(idSemillero, vector);
                        }
                    });
        }

        // 3. Punto de consulta: centroide ponderado del historial o, si no hay, el perfil
        Base base = Base.HISTORIAL;
        double[] consulta = centroidePonderado(vectorPorSemillero, pesoPorSemillero, indice.dimensiones().size());
        if (consulta == null) {
            base = Base.PERFIL;
            consulta = indice.vectorizar(estudianteLineaRepository.findAreaIdsByEstudianteId(idEstudiante));
        }

        Map<Integer, Semillero> semillerosHistorial = semilleroRepository.findAllById(pesoPorSemillero.keySet())
                .stream().collect(Collectors.toMap(Semillero::getId, Function.identity()));
        List<SemilleroReferencia> referencias = pesoPorSemillero.entrySet().stream()
                .filter(entrada -> semillerosHistorial.containsKey(entrada.getKey()))
                .map(entrada -> SemilleroReferencia.builder()
                        .idSemillero(entrada.getKey())
                        .nombre(semillerosHistorial.get(entrada.getKey()).getNombre())
                        .actual(entrada.getValue() == pesos.actual())
                        .peso(entrada.getValue())
                        .build())
                .toList();

        KdTree<Integer> arbol = indice.arbol();
        if (consulta == null || arbol.estaVacio()) {
            return SemillerosSimilaresResponseDTO.builder()
                    .base(consulta == null ? Base.SIN_DATOS : base)
                    .referencias(referencias)
                    .similares(List.of())
                    .busqueda(estadisticas(indice, 0))
                    .build();
        }

        // 4. Búsqueda k-NN: se descartan los semilleros del historial durante el recorrido.
        //    Con pocas dimensiones hay muchos empates (semilleros con las mismas áreas), así que
        //    se piden más vecinos de los necesarios y se desempata en el paso 5.
        int candidatos = k * CANDIDATOS_POR_RESULTADO;
        KdTree.ResultadoBusqueda<Integer> resultado = arbol.buscarVecinosMasCercanos(
                consulta, candidatos, idSemillero -> !pesoPorSemillero.containsKey(idSemillero));
        List<Integer> idsVecinos = resultado.vecinos().stream().map(KdTree.Vecino::valor).toList();

        Map<Integer, Semillero> encontrados = semilleroRepository.findAllById(idsVecinos)
                .stream().collect(Collectors.toMap(Semillero::getId, Function.identity()));
        Map<Integer, Map<Integer, String>> lineasPorVecino = lineasPorSemillero(idsVecinos);
        Set<Integer> conConvocatoriaAbierta =
                convocatoriaRepository.idsSemillerosRecibiendoPostulaciones(idsVecinos, LocalDate.now());
        Set<Integer> lineasReferencia = base == Base.HISTORIAL
                ? lineasPorSemillero(pesoPorSemillero.keySet()).values().stream()
                        .flatMap(lineas -> lineas.keySet().stream()).collect(Collectors.toSet())
                : estudianteLineaRepository.findByEstudianteId(idEstudiante).stream()
                        .map(el -> el.getLineaInvestigacion().getId()).collect(Collectors.toSet());

        List<SemilleroSimilar> similares = new ArrayList<>();
        for (KdTree.Vecino<Integer> vecino : resultado.vecinos()) {
            Semillero semillero = encontrados.get(vecino.valor());
            // El árbol puede tener hasta "vigencia" minutos: se descartan los que dejaron de estar activos
            if (semillero == null || !semillero.estaActivo()) {
                continue;
            }
            // Vectores unitarios: cos = 1 - d²/2. Coseno 0 = ninguna área en común.
            double similitud = 1 - vecino.distanciaCuadrada() / 2;
            if (similitud <= 1e-9) {
                continue;
            }
            List<String> lineasEnComun = lineasPorVecino.getOrDefault(semillero.getId(), Map.of()).entrySet()
                    .stream()
                    .filter(linea -> lineasReferencia.contains(linea.getKey()))
                    .map(Map.Entry::getValue)
                    .sorted()
                    .toList();
            similares.add(SemilleroSimilar.builder()
                    .idSemillero(semillero.getId())
                    .nombre(semillero.getNombre())
                    .codigoIdentificador(semillero.getCodigoIdentificador())
                    .nombrePrograma(semillero.getPrograma() != null ? semillero.getPrograma().getNombre() : null)
                    .porcentajeSimilitud(Redondeo.unDecimal(similitud * 100))
                    .distancia(Redondeo.tresDecimales(vecino.distancia()))
                    .areasEnComun(areasEnComun(indice, consulta, vecino.punto()))
                    .lineasEnComun(lineasEnComun)
                    .parecidoA(base == Base.HISTORIAL
                            ? masParecidoDelHistorial(vecino.punto(), vectorPorSemillero, semillerosHistorial)
                            : null)
                    .convocatoriaAbierta(conConvocatoriaAbierta.contains(semillero.getId()))
                    .build());
        }

        // 5. Orden final: similitud del árbol; a igual similitud, más líneas en común y luego
        //    convocatoria abierta (el estudiante puede postularse ya)
        List<SemilleroSimilar> mejores = similares.stream()
                .sorted(Comparator.comparingDouble(SemilleroSimilar::getPorcentajeSimilitud).reversed()
                        .thenComparing(similar -> similar.getLineasEnComun().size(), Comparator.reverseOrder())
                        .thenComparing(SemilleroSimilar::isConvocatoriaAbierta, Comparator.reverseOrder())
                        .thenComparing(SemilleroSimilar::getNombre))
                .limit(k)
                .toList();

        BusquedaKd busqueda = estadisticas(indice, resultado.nodosVisitados());
        busqueda.setCandidatosEvaluados(resultado.vecinos().size());
        return SemillerosSimilaresResponseDTO.builder()
                .base(base)
                .referencias(referencias)
                .similares(mejores)
                .busqueda(busqueda)
                .build();
    }

    /** idSemillero → (idLínea → nombre), en una sola consulta. */
    private Map<Integer, Map<Integer, String>> lineasPorSemillero(Collection<Integer> idsSemillero) {
        if (idsSemillero.isEmpty()) {
            return Map.of();
        }
        return semilleroLineaRepository.findLineasDeSemilleros(idsSemillero).stream()
                .collect(Collectors.groupingBy(SemilleroLineaInvestigacionRepository.LineaDeSemillero::getIdSemillero,
                        Collectors.toMap(SemilleroLineaInvestigacionRepository.LineaDeSemillero::getIdLinea,
                                SemilleroLineaInvestigacionRepository.LineaDeSemillero::getNombreLinea,
                                (a, b) -> a)));
    }

    /** Σ peso·vector de cada semillero del historial, normalizado. Null si no hay vectores. */
    private static double[] centroidePonderado(Map<Integer, double[]> vectorPorSemillero,
                                               Map<Integer, Double> pesoPorSemillero, int dimensiones) {
        if (vectorPorSemillero.isEmpty()) {
            return null;
        }
        double[] suma = new double[dimensiones];
        vectorPorSemillero.forEach((idSemillero, vector) -> {
            double peso = pesoPorSemillero.get(idSemillero);
            for (int i = 0; i < dimensiones; i++) {
                suma[i] += peso * vector[i];
            }
        });
        return IndiceSemillerosKdService.normalizar(suma);
    }

    private static List<String> areasEnComun(Indice indice, double[] consulta, double[] punto) {
        List<String> areas = new ArrayList<>();
        for (int i = 0; i < punto.length; i++) {
            if (consulta[i] > 0 && punto[i] > 0) {
                areas.add(indice.dimensiones().get(i).nombreArea());
            }
        }
        return areas;
    }

    /** Semillero del historial con mayor similitud coseno al recomendado. */
    private static String masParecidoDelHistorial(double[] punto, Map<Integer, double[]> vectorPorSemillero,
                                                  Map<Integer, Semillero> semillerosHistorial) {
        String nombre = null;
        double mejor = 0;
        for (Map.Entry<Integer, double[]> entrada : vectorPorSemillero.entrySet()) {
            double coseno = IndiceSemillerosKdService.productoPunto(punto, entrada.getValue());
            Semillero semillero = semillerosHistorial.get(entrada.getKey());
            if (coseno > mejor && semillero != null) {
                mejor = coseno;
                nombre = semillero.getNombre();
            }
        }
        return nombre;
    }

    private static BusquedaKd estadisticas(Indice indice, int nodosVisitados) {
        return BusquedaKd.builder()
                .dimensiones(indice.dimensiones().size())
                .nombresDimensiones(indice.dimensiones().stream()
                        .map(IndiceSemillerosKdService.Dimension::nombreArea).toList())
                .semillerosEnArbol(indice.arbol().tamano())
                .profundidadArbol(indice.arbol().profundidad())
                .nodosVisitados(nodosVisitados)
                .build();
    }

}
