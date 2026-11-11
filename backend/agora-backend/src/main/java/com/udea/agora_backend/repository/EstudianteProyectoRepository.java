package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EstudianteProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteProyectoRepository extends JpaRepository<EstudianteProyecto, Integer> {
    List<EstudianteProyecto> findByProyectoId(Integer proyectoId);
    void deleteByProyectoId(Integer proyectoId);
    List<EstudianteProyecto> findByEstudianteId(Integer estudianteId);
    Optional<EstudianteProyecto> findByProyectoIdAndEstudianteId(Integer proyectoId, Integer estudianteId);


    /** Total de estudiantes vinculados a un proyecto. */
    interface IntegrantesDeProyecto {
        Integer getIdProyecto();
        Long getTotal();
    }

    @Query("""
            SELECT ep.proyecto.id AS idProyecto, COUNT(ep) AS total
            FROM EstudianteProyecto ep
            WHERE ep.proyecto.id IN :idsProyecto
            GROUP BY ep.proyecto.id
            """)
    List<IntegrantesDeProyecto> contarPorProyecto(@Param("idsProyecto") Collection<Integer> idsProyecto);
}
