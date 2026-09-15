package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.SemilleroLineaInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemilleroLineaInvestigacionRepository extends JpaRepository<SemilleroLineaInvestigacion, Integer> {
    List<SemilleroLineaInvestigacion> findBySemilleroId(Integer semilleroId);
    Optional<SemilleroLineaInvestigacion> findBySemilleroIdAndLineaInvestigacionId(Integer semilleroId, Integer lineaInvestigacionId);
}
