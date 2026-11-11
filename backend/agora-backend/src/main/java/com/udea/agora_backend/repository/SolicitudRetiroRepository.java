package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.SolicitudRetiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SolicitudRetiroRepository extends JpaRepository<SolicitudRetiro, Integer> {

    /** Solicitudes de un semillero, las más recientes primero. */
    @Query("""
            SELECT s FROM SolicitudRetiro s
            WHERE s.vinculacion.semillero.id = :idSemillero
            ORDER BY s.fechaSolicitud DESC
            """)
    List<SolicitudRetiro> findBySemillero(@Param("idSemillero") Integer idSemillero);

    /** Solicitudes de un estudiante en todos sus semilleros, las más recientes primero. */
    @Query("""
            SELECT s FROM SolicitudRetiro s
            WHERE s.vinculacion.estudiante.id = :idEstudiante
            ORDER BY s.fechaSolicitud DESC
            """)
    List<SolicitudRetiro> findByEstudiante(@Param("idEstudiante") Integer idEstudiante);

    @Query("SELECT s FROM SolicitudRetiro s WHERE s.vinculacion.id = :idVinculacion AND s.estado.nombre = :estado")
    List<SolicitudRetiro> findByVinculacionYEstado(@Param("idVinculacion") Integer idVinculacion,
                                                   @Param("estado") String estado);

    @Query("""
            SELECT COUNT(s) FROM SolicitudRetiro s
            WHERE s.vinculacion.semillero.id IN :idsSemillero AND s.estado.nombre = :estado
            """)
    long contarPorSemillerosYEstado(@Param("idsSemillero") Collection<Integer> idsSemillero,
                                    @Param("estado") String estado);
}
