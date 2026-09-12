package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.LineaInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LineaInvestigacionRepository extends JpaRepository<LineaInvestigacion, Integer> {
    Optional<LineaInvestigacion> findByNombre(String nombre);
}
