package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Integer> {
    List<Estado> findByCategoriaEstadoId(Integer categoriaEstadoId);

    // Los nombres de estado no son únicos entre categorías: buscar siempre con la categoría
    Optional<Estado> findByCategoriaEstadoNombreIgnoreCaseAndNombreIgnoreCase(String categoria, String nombre);

    List<Estado> findByNombreIgnoreCase(String nombre);

    List<Estado> findByCategoriaEstadoNombreIgnoreCase(String categoria);
}
