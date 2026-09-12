package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository 
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer>{
    Optional<Estudiante> findByUsuarioId(Integer usuarioId);

}
