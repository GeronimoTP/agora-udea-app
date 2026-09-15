package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Integer> {
    List<Estado> findByCategoriaEstadoId(Integer categoriaEstadoId);
    Optional<Estado> findByNombreIgnoreCase(String nombre);
}
