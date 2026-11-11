package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.RequisitoConvocatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RequisitoConvocatoriaRepository extends JpaRepository<RequisitoConvocatoria, Integer> {

    /** Requisitos de varias convocatorias (con su habilidad), en el orden en que se definieron. */
    @Query("""
            SELECT r FROM RequisitoConvocatoria r
            LEFT JOIN FETCH r.habilidad
            WHERE r.convocatoria.id IN :idsConvocatoria
            ORDER BY r.id
            """)
    List<RequisitoConvocatoria> findByConvocatoriaIdIn(@Param("idsConvocatoria") Collection<Integer> idsConvocatoria);

    default List<RequisitoConvocatoria> findByConvocatoriaId(Integer idConvocatoria) {
        return findByConvocatoriaIdIn(List.of(idConvocatoria));
    }
}
