package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.ProfesorAreaEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesorAreaEspecialidadRepository extends JpaRepository<ProfesorAreaEspecialidad, Integer> {
    List<ProfesorAreaEspecialidad> findByProfesorId(Integer profesorId);
    Optional<ProfesorAreaEspecialidad> findByProfesorIdAndAreaEspecialidadId(Integer profesorId, Integer areaEspecialidadId);


    /** Áreas de varios profesores (con su nombre), en el orden en que se registraron. */
    @Query("""
            SELECT pae FROM ProfesorAreaEspecialidad pae
            JOIN FETCH pae.areaEspecialidad
            WHERE pae.profesor.id IN :idsProfesor
            ORDER BY pae.id
            """)
    List<ProfesorAreaEspecialidad> findByProfesorIdIn(@Param("idsProfesor") Collection<Integer> idsProfesor);
}
