package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository 
public interface ProyectoRepository extends JpaRepository<Proyecto, Integer> {

    List<Proyecto> findBySemilleroId(Integer semilleroId);
    
}
