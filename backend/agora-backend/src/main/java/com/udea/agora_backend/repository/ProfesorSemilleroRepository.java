package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.ProfesorSemillero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesorSemilleroRepository extends JpaRepository<ProfesorSemillero, Integer> {
    List<ProfesorSemillero> findBySemilleroId(Integer semilleroId);
    List<ProfesorSemillero> findByProfesorId(Integer profesorId);
    @Query("SELECT ps.semillero.id FROM ProfesorSemillero ps WHERE ps.profesor.id = :profesorId")
    List<Integer> findSemilleroIdsByProfesorId(@Param("profesorId") Integer profesorId);
    Optional<ProfesorSemillero> findBySemilleroIdAndProfesorId(Integer semilleroId, Integer profesorId);


    /** Par (semillero, área de especialidad de uno de sus tutores). */
    interface AreaDeTutor {
        Integer getIdSemillero();
        Integer getIdArea();
    }

    // Áreas de los tutores de varios semilleros en una sola consulta
    @Query("""
            SELECT ps.semillero.id AS idSemillero, pae.areaEspecialidad.id AS idArea
            FROM ProfesorSemillero ps
            JOIN ProfesorAreaEspecialidad pae ON pae.profesor = ps.profesor
            WHERE ps.semillero.id IN :idsSemillero
            """)
    List<AreaDeTutor> findAreasDeTutores(@Param("idsSemillero") Collection<Integer> idsSemillero);
}
