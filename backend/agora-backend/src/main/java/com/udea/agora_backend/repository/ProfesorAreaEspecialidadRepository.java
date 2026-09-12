package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.ProfesorAreaEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfesorAreaEspecialidadRepository extends JpaRepository<ProfesorAreaEspecialidad, Integer> {
    List<ProfesorAreaEspecialidad> findByProfesorId(Integer profesorId);
}
