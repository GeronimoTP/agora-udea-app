package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.OauthProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OauthProveedorRepository extends JpaRepository<OauthProveedor, Integer> {
    Optional<OauthProveedor> findByNombre(String nombre);
}
