package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.ReconocimientoSemillero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconocimientoSemilleroRepository extends JpaRepository<ReconocimientoSemillero, Integer> {
    List<ReconocimientoSemillero> findBySemilleroId(Integer semilleroId);
}
