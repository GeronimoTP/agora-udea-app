package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.TipoReconocimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoReconocimientoRepository extends JpaRepository<TipoReconocimiento, Integer> {
}
