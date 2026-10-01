package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.ProfesorSemillero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesorSemilleroRepository extends JpaRepository<ProfesorSemillero, Integer> {
    List<ProfesorSemillero> findBySemilleroId(Integer semilleroId);
    List<ProfesorSemillero> findByProfesorId(Integer profesorId);
    @Query("SELECT ps.semillero.id FROM ProfesorSemillero ps WHERE ps.profesor.id = :profesorId")
    List<Integer> findSemilleroIdsByProfesorId(@Param("profesorId") Integer profesorId);
    Optional<ProfesorSemillero> findBySemilleroIdAndProfesorId(Integer semilleroId, Integer profesorId);
}
