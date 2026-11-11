package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.repository.AreaEspecialidadRepository;
import com.udea.agora_backend.repository.EntidadOtorganteRepository;
import com.udea.agora_backend.repository.HabilidadRepository;
import com.udea.agora_backend.repository.LineaInvestigacionRepository;
import com.udea.agora_backend.repository.TipoReconocimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evita elementos del catálogo con el mismo nombre (sin distinguir mayúsculas). Lo usan la
 * creación directa del administrador y las solicitudes de catálogo. Sin esta validación, el
 * UNIQUE de la base respondería con un error 500.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NombresCatalogo {

    private final HabilidadRepository habilidadRepository;
    private final LineaInvestigacionRepository lineaRepository;
    private final AreaEspecialidadRepository areaRepository;
    private final EntidadOtorganteRepository entidadRepository;
    private final TipoReconocimientoRepository tipoReconocimientoRepository;

    public boolean existe(TipoElementoCatalogo tipo, String nombre) {
        String limpio = nombre.trim();
        return switch (tipo) {
            case HABILIDAD -> habilidadRepository.existsByNombreIgnoreCase(limpio);
            case LINEA_INVESTIGACION -> lineaRepository.existsByNombreIgnoreCase(limpio);
            case AREA_ESPECIALIDAD -> areaRepository.existsByNombreIgnoreCase(limpio);
            case ENTIDAD_OTORGANTE -> entidadRepository.existsByNombreIgnoreCase(limpio);
            case TIPO_RECONOCIMIENTO -> tipoReconocimientoRepository.existsByNombreIgnoreCase(limpio);
        };
    }

    public void validarLibre(TipoElementoCatalogo tipo, String nombre) {
        if (existe(tipo, nombre)) {
            throw new ConflictoException(tipo.getEtiqueta() + " '" + nombre.trim() + "' ya existe en el catálogo.");
        }
    }

    /** Al renombrar: solo valida si el nombre cambió (además de mayúsculas). */
    public void validarRenombre(TipoElementoCatalogo tipo, String nombreActual, String nombreNuevo) {
        if (!nombreActual.trim().equalsIgnoreCase(nombreNuevo.trim())) {
            validarLibre(tipo, nombreNuevo);
        }
    }
}
