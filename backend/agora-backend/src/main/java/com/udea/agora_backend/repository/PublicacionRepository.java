package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, Integer> {
    List<Publicacion> findByProyectoId(Integer proyectoId);
    List<Publicacion> findByProyectoSemilleroId(Integer semilleroId);

    @Query("""
            SELECT p.proyecto.semillero.id AS idSemillero, COUNT(p) AS total
            FROM Publicacion p
            WHERE p.proyecto.semillero.id IN :idsSemillero
            GROUP BY p.proyecto.semillero.id
            """)
    List<ConteoPorSemillero> contarPorSemillero(@Param("idsSemillero") Collection<Integer> idsSemillero);
}
