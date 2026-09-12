package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EstudianteSemillero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteSemilleroRepository extends JpaRepository<EstudianteSemillero, Integer> {
    List<EstudianteSemillero> findBySemilleroId(Integer semilleroId);
    List<EstudianteSemillero> findByEstudianteId(Integer estudianteId);
}
