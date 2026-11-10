package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosConvocatoria;
import com.udea.agora_backend.dominio.EstadosPostulacion;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.repository.PostulacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Marca como Expirada lo que el flujo de doble vía deja sin resolver: postulaciones de una
 * convocatoria que se cerró y ofertas que el estudiante no respondió a tiempo. La usan el
 * gestor de convocatorias (cierres y pre-aprobaciones) y las tareas programadas.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpiracionPostulacionesService {

    private final PostulacionRepository postulacionRepository;
    private final BuscadorEstados buscadorEstados;
    private final PlazoOfertasService plazoOfertasService;

    /** Expira lo que quedó en revisión o con oferta pendiente en una convocatoria cerrada. */
    public int expirarSinResolver(Integer idConvocatoria) {
        List<Postulacion> sinResolver = postulacionRepository.findPostulacionesByConvocatoriaAndEstado(
                idConvocatoria, EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA);
        sinResolver.forEach(postulacion ->
                log.info("Marcando postulación {} como expirada", postulacion.getId()));
        return expirar(sinResolver);
    }

    /** Lo mismo para todas las convocatorias que ya estaban cerradas (sin cupos o cierre manual). */
    public int expirarSinResolverEnConvocatoriasCerradas() {
        return expirar(postulacionRepository.findPostulacionesEnConvocatoriasConEstado(
                EstadosConvocatoria.CERRADA, EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA));
    }

    /**
     * Expira las ofertas cuyo plazo de respuesta venció. Conserva fechaPreAprobacion: cuenta como
     * oferta expirada en la analítica de retención.
     */
    public int expirarOfertasVencidas() {
        List<Postulacion> vencidas = postulacionRepository.findOfertasVencidas(
                EstadosPostulacion.PRE_APROBADA, inicioOfertasVigentes());
        vencidas.forEach(oferta ->
                log.info("Oferta {} expirada: el estudiante no respondió en el plazo", oferta.getId()));
        return expirar(vencidas);
    }

    /** Las ofertas vencidas que el cron aún no procesó no deben ocupar espacio en la convocatoria. */
    public int expirarOfertasVencidasDeConvocatoria(Integer idConvocatoria) {
        List<Postulacion> vencidas = postulacionRepository.findOfertasVencidasDeConvocatoria(
                idConvocatoria, EstadosPostulacion.PRE_APROBADA, inicioOfertasVigentes());
        vencidas.forEach(oferta ->
                log.info("Oferta {} expirada: el estudiante no respondió en el plazo", oferta.getId()));
        return expirar(vencidas);
    }

    private ZonedDateTime inicioOfertasVigentes() {
        return plazoOfertasService.inicioOfertasVigentes(ZonedDateTime.now());
    }

    private int expirar(List<Postulacion> postulaciones) {
        if (postulaciones.isEmpty()) {
            return 0;
        }
        Estado expirada = buscadorEstados.dePostulacion(EstadosPostulacion.EXPIRADA);
        for (Postulacion postulacion : postulaciones) {
            postulacion.setEstado(expirada);
            postulacionRepository.save(postulacion);
        }
        return postulaciones.size();
    }
}
