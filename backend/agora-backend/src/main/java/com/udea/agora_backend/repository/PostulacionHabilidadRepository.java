package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.PostulacionHabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostulacionHabilidadRepository extends JpaRepository<PostulacionHabilidad, Integer> {
    List<PostulacionHabilidad> findByPostulacionId(Integer postulacionId);
}
