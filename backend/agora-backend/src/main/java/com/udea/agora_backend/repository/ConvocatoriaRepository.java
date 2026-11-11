package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Convocatoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer> {
    List<Convocatoria> findBySemilleroId(Integer semilleroId);
    List<Convocatoria> findBySemilleroIdIn(Collection<Integer> idsSemillero);
    List<Convocatoria> findByEstadoId(Integer estadoId);

    // Convocatorias activas con cupos disponibles
    @Query("SELECT c FROM Convocatoria c WHERE c.cuposDisponibles > 0 AND c.estado.id = ?1")
    List<Convocatoria> findActivasConCupos(Integer estadoId);

    // Convocatorias abiertas que necesitan ser cerradas (fecha expirada); las canceladas no se tocan
    @Query("SELECT c FROM Convocatoria c WHERE c.fechaCierre < CURRENT_DATE AND c.estado.nombre = 'Abierta'")
    List<Convocatoria> findConvocatoriasExpiradas();

    // Bloqueo de fila para descontar cupos sin condiciones de carrera entre aceptaciones simultáneas
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Convocatoria c WHERE c.id = ?1")
    Optional<Convocatoria> findByIdParaActualizarCupos(Integer id);

    /**
     * De los semilleros dados, los que tienen hoy al menos una convocatoria abierta, iniciada,
     * vigente y con cupos. Una sola consulta para todos.
     */
    default Set<Integer> idsSemillerosRecibiendoPostulaciones(Collection<Integer> idsSemillero, LocalDate hoy) {
        if (idsSemillero.isEmpty()) {
            return Set.of();
        }
        return findBySemilleroIdIn(idsSemillero).stream()
                .filter(c -> c.recibePostulaciones(hoy))
                .map(c -> c.getSemillero().getId())
                .collect(Collectors.toSet());
    }
}
