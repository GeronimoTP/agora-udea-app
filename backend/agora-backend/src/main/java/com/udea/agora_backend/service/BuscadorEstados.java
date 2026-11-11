package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.CategoriasEstado;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.repository.EstadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único punto para obtener estados del catálogo. Los nombres se repiten entre categorías
 * ("Activo", "Expirada"...), así que toda búsqueda exige la categoría.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuscadorEstados {

    private final EstadoRepository estadoRepository;

    public Estado porNombre(String categoria, String nombre) {
        return estadoRepository.findByCategoriaEstadoNombreIgnoreCaseAndNombreIgnoreCase(categoria, nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", categoria + " / " + nombre));
    }

    /** Estado elegido por el usuario: debe existir y pertenecer a la categoría esperada. */
    public Estado porIdEnCategoria(String categoria, Integer idEstado) {
        Estado estado = estadoRepository.findById(idEstado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", idEstado));
        if (!categoria.equalsIgnoreCase(estado.getCategoriaEstado().getNombre())) {
            throw new ErrorLogicaNegocioException(
                    "El estado '" + estado.getNombre() + "' no pertenece a la categoría '" + categoria + "'.");
        }
        return estado;
    }

    public Estado dePostulacion(String nombre) {
        return porNombre(CategoriasEstado.POSTULACION, nombre);
    }

    public Estado deConvocatoria(String nombre) {
        return porNombre(CategoriasEstado.CONVOCATORIA, nombre);
    }

    public Estado deSemillero(String nombre) {
        return porNombre(CategoriasEstado.SEMILLERO, nombre);
    }

    public Estado deVinculacion(String nombre) {
        return porNombre(CategoriasEstado.VINCULACION, nombre);
    }

    public Estado deSolicitud(String nombre) {
        return porNombre(CategoriasEstado.SOLICITUD, nombre);
    }
}
