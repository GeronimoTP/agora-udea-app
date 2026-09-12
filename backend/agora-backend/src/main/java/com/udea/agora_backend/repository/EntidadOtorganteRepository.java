package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EntidadOtorgante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntidadOtorganteRepository extends JpaRepository<EntidadOtorgante, Integer> {
}