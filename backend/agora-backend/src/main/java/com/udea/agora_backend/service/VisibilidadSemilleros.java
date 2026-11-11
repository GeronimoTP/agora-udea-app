package com.udea.agora_backend.service;

import com.udea.agora_backend.model.Semillero;

import java.util.Set;

/**
 * Qué semilleros no aprobados (pendientes o rechazados) puede ver un usuario: el administrador
 * todos y cada líder los suyos. Los aprobados los ve cualquiera.
 */
public record VisibilidadSemilleros(boolean administrador, Set<Integer> liderados) {

    public static VisibilidadSemilleros soloAprobados() {
        return new VisibilidadSemilleros(false, Set.of());
    }

    public boolean puedeVer(Semillero semillero) {
        return semillero.estaAprobado() || administrador || liderados.contains(semillero.getId());
    }
}
