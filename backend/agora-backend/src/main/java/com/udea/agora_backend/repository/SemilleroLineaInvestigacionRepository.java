package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.SemilleroLineaInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemilleroLineaInvestigacionRepository extends JpaRepository<SemilleroLineaInvestigacion, Integer> {
    List<SemilleroLineaInvestigacion> findBySemilleroId(Integer semilleroId);
    Optional<SemilleroLineaInvestigacion> findBySemilleroIdAndLineaInvestigacionId(Integer semilleroId, Integer lineaInvestigacionId);

    /** Par (semillero, área de especialidad de una de sus líneas). Un par por cada línea. */
    interface AreaDeSemillero {
        Integer getIdSemillero();
        Integer getIdArea();
    }

    // Áreas de las líneas de todos los semilleros en un estado dado: alimenta el árbol KD
    // de semilleros similares en una sola consulta
    @Query("""
            SELECT s.id AS idSemillero, l.areaEspecialidad.id AS idArea
            FROM SemilleroLineaInvestigacion sl
            JOIN sl.lineaInvestigacion l
            JOIN sl.semillero s
            JOIN s.estado e
            JOIN e.categoriaEstado c
            WHERE LOWER(e.nombre) = LOWER(:estado) AND c.nombre = :categoria
            """)
    List<AreaDeSemillero> findAreasDeSemillerosPorEstado(@Param("estado") String estado,
                                                         @Param("categoria") String categoria);

    @Query("""
            SELECT sl.semillero.id AS idSemillero, l.areaEspecialidad.id AS idArea
            FROM SemilleroLineaInvestigacion sl
            JOIN sl.lineaInvestigacion l
            WHERE sl.semillero.id IN :idsSemillero
            """)
    List<AreaDeSemillero> findAreasDeSemilleros(@Param("idsSemillero") Collection<Integer> idsSemillero);

    /** Par (semillero, línea de investigación). */
    interface LineaDeSemillero {
        Integer getIdSemillero();
        Integer getIdLinea();
        String getNombreLinea();
    }

    @Query("""
            SELECT sl.semillero.id AS idSemillero, l.id AS idLinea, l.nombre AS nombreLinea
            FROM SemilleroLineaInvestigacion sl
            JOIN sl.lineaInvestigacion l
            WHERE sl.semillero.id IN :idsSemillero
            """)
    List<LineaDeSemillero> findLineasDeSemilleros(@Param("idsSemillero") Collection<Integer> idsSemillero);
}
