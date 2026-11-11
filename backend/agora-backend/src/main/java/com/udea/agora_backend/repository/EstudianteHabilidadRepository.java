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
