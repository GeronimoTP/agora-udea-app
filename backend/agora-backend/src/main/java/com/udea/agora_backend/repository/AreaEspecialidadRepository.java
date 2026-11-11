package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.AreaEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaEspecialidadRepository extends JpaRepository<AreaEspecialidad, Integer> {
    Optional<AreaEspecialidad> findByNombre(String nombre);
    List<AreaEspecialidad> findByNombreContainingIgnoreCaseOrderByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
