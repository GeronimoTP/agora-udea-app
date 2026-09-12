package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostulacionRepository extends JpaRepository<Postulacion, Integer> {
    List<Postulacion> findByConvocatoriaId(Integer convocatoriaId);
    List<Postulacion> findByEstudianteId(Integer estudianteId);
}
