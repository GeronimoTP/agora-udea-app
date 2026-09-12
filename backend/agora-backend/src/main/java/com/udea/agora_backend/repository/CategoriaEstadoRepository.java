package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.CategoriaEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaEstadoRepository extends JpaRepository<CategoriaEstado, Integer> {
    Optional<CategoriaEstado> findByNombre(String nombre);
}