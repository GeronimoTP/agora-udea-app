package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.RolEstudianteProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolEstudianteProyectoRepository extends JpaRepository<RolEstudianteProyecto, Integer> {
}
