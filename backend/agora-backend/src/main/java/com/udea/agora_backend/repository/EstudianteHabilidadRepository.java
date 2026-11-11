package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EstudianteHabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EstudianteHabilidadRepository extends JpaRepository<EstudianteHabilidad, Integer> {
    List<EstudianteHabilidad> findByEstudianteId(Integer estudianteId);

    /** Par (semillero, habilidad de perfil de alguien de su equipo). */
    interface HabilidadDeEquipo {
        Integer getIdSemillero();
        Integer getIdHabilidad();
    }

    // Habilidades de los integrantes activos de varios semilleros
    @Query("""
            SELECT es.semillero.id AS idSemillero, eh.habilidad.id AS idHabilidad
            FROM EstudianteSemillero es
            JOIN EstudianteHabilidad eh ON eh.estudiante = es.estudiante
            WHERE es.semillero.id IN :idsSemillero AND es.fechaSalida IS NULL
            """)
    List<HabilidadDeEquipo> findHabilidadesDeIntegrantes(@Param("idsSemillero") Collection<Integer> idsSemillero);

    // Habilidades del estudiante líder de varios semilleros
    @Query("""
            SELECT s.id AS idSemillero, eh.habilidad.id AS idHabilidad
            FROM Semillero s
            JOIN EstudianteHabilidad eh ON eh.estudiante = s.estudianteLider
            WHERE s.id IN :idsSemillero
            """)
    List<HabilidadDeEquipo> findHabilidadesDeLideres(@Param("idsSemillero") Collection<Integer> idsSemillero);

    @Query("SELECT eh.habilidad.id FROM EstudianteHabilidad eh WHERE eh.estudiante.id = :estudianteId")
    List<Integer> findHabilidadIdsByEstudianteId(@Param("estudianteId") Integer estudianteId);


    /** Habilidades del perfil de varios estudiantes (con su nombre), en el orden en que se registraron. */
    @Query("""
            SELECT eh FROM EstudianteHabilidad eh
            JOIN FETCH eh.habilidad
            WHERE eh.estudiante.id IN :idsEstudiante
            ORDER BY eh.id
            """)
    List<EstudianteHabilidad> findByEstudianteIdIn(@Param("idsEstudiante") Collection<Integer> idsEstudiante);
}
