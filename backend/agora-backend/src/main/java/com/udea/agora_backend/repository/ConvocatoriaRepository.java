package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Convocatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer> {
    List<Convocatoria> findBySemilleroId(Integer semilleroId);
    List<Convocatoria> findByEstadoId(Integer estadoId);
}
