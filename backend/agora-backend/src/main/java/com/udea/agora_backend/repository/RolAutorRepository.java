package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.RolAutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolAutorRepository extends JpaRepository<RolAutor, Integer> {
}
