package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Integer> {

    List<Proyecto> findBySemilleroId(Integer semilleroId);

    @Query("""
            SELECT p.semillero.id AS idSemillero, COUNT(p) AS total
            FROM Proyecto p
            WHERE p.semillero.id IN :idsSemillero
            GROUP BY p.semillero.id
            """)
    List<ConteoPorSemillero> contarPorSemillero(@Param("idsSemillero") Collection<Integer> idsSemillero);

}
