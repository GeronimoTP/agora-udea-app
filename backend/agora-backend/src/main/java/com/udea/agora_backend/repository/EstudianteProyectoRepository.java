package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EstudianteProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteProyectoRepository extends JpaRepository<EstudianteProyecto, Integer> {
    List<EstudianteProyecto> findByProyectoId(Integer proyectoId);
    List<EstudianteProyecto> findByEstudianteId(Integer estudianteId);
}
