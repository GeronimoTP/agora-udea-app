package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.PublicacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicacionUsuarioRepository extends JpaRepository<PublicacionUsuario, Integer> {
    List<PublicacionUsuario> findByPublicacionId(Integer publicacionId);
    List<PublicacionUsuario> findByUsuarioId(Integer usuarioId);
    Optional<PublicacionUsuario> findByPublicacionIdAndUsuarioId(Integer publicacionId, Integer usuarioId);
}
