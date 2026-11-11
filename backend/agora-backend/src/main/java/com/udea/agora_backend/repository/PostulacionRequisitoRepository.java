package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.PostulacionRequisito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostulacionRequisitoRepository extends JpaRepository<PostulacionRequisito, Integer> {

    /** Par (postulación, requisito de texto que el estudiante declaró cumplir). */
    interface RequisitoDeclarado {
        Integer getIdPostulacion();
        Integer getIdRequisito();
    }

    @Query("""
            SELECT pr.postulacion.id AS idPostulacion, pr.requisito.id AS idRequisito
            FROM PostulacionRequisito pr
            WHERE pr.postulacion.id IN :idsPostulacion
            """)
    List<RequisitoDeclarado> findDeclarados(@Param("idsPostulacion") Collection<Integer> idsPostulacion);

    List<PostulacionRequisito> findByPostulacionId(Integer idPostulacion);
}
