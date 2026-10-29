package com.udea.agora_backend.service;

import com.udea.agora_backend.model.Postulacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

/**
 * Plazo que tiene el estudiante para responder una oferta (postulación Pre-aprobada).
 * Se cuenta desde fechaPreAprobacion y es configurable con app.ofertas.dias-respuesta.
 * Vencido el plazo, la oferta expira (cron de GestorConvocatoriasService) y libera el
 * espacio para que el líder pueda ofrecerlo a otro aspirante.
 */
@Service
public class PlazoOfertasService {

    private final int diasRespuesta;

    public PlazoOfertasService(@Value("${app.ofertas.dias-respuesta:3}") int diasRespuesta) {
        if (diasRespuesta < 1) {
            throw new IllegalArgumentException("app.ofertas.dias-respuesta debe ser al menos 1");
        }
        this.diasRespuesta = diasRespuesta;
    }

    public int getDiasRespuesta() {
        return diasRespuesta;
    }

    /** Fecha límite de respuesta; null si la postulación nunca recibió oferta. */
    public ZonedDateTime calcularFechaLimite(Postulacion postulacion) {
        return postulacion.getFechaPreAprobacion() == null
                ? null
                : postulacion.getFechaPreAprobacion().plusDays(diasRespuesta);
    }

    public boolean estaVencida(Postulacion postulacion, ZonedDateTime ahora) {
        ZonedDateTime limite = calcularFechaLimite(postulacion);
        return limite != null && limite.isBefore(ahora);
    }

    /** Las ofertas emitidas antes de este instante ya vencieron. */
    public ZonedDateTime inicioOfertasVigentes(ZonedDateTime ahora) {
        return ahora.minusDays(diasRespuesta);
    }
}
