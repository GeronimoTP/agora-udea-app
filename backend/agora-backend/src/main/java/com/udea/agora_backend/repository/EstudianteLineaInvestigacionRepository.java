package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EstudianteLineaInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteLineaInvestigacionRepository extends JpaRepository<EstudianteLineaInvestigacion, Integer> {
    List<EstudianteLineaInvestigacion> findByEstudianteId(Integer estudianteId);
}
