package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EstudianteLineaInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteLineaInvestigacionRepository extends JpaRepository<EstudianteLineaInvestigacion, Integer> {
    List<EstudianteLineaInvestigacion> findByEstudianteId(Integer estudianteId);
    Optional<EstudianteLineaInvestigacion> findByEstudianteIdAndLineaInvestigacionId(Integer estudianteId, Integer lineaInvestigacionId);

    // Área de especialidad de cada línea de interés del estudiante (con repeticiones)
    @Query("""
            SELECT l.areaEspecialidad.id FROM EstudianteLineaInvestigacion el
            JOIN el.lineaInvestigacion l
            WHERE el.estudiante.id = :idEstudiante
            """)
    List<Integer> findAreaIdsByEstudianteId(@Param("idEstudiante") Integer idEstudiante);


    /** Líneas de interés de varios estudiantes (con su línea cargada), en el orden en que se registraron. */
    @Query("""
            SELECT el FROM EstudianteLineaInvestigacion el
            JOIN FETCH el.lineaInvestigacion
            WHERE el.estudiante.id IN :idsEstudiante
            ORDER BY el.id
            """)
    List<EstudianteLineaInvestigacion> findByEstudianteIdIn(@Param("idsEstudiante") Collection<Integer> idsEstudiante);
}
