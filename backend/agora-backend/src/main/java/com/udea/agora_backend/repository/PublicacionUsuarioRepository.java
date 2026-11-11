package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.PublicacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicacionUsuarioRepository extends JpaRepository<PublicacionUsuario, Integer> {
    List<PublicacionUsuario> findByPublicacionId(Integer publicacionId);
    void deleteByPublicacionId(Integer publicacionId);
    List<PublicacionUsuario> findByUsuarioId(Integer usuarioId);
    Optional<PublicacionUsuario> findByPublicacionIdAndUsuarioId(Integer publicacionId, Integer usuarioId);

    List<PublicacionUsuario> findByPublicacionProyectoSemilleroId(Integer semilleroId);


    /** Autorías de varias publicaciones (con usuario y rol), en el orden en que se registraron. */
    @Query("""
            SELECT pu FROM PublicacionUsuario pu
            JOIN FETCH pu.usuario
            JOIN FETCH pu.rolAutor
            WHERE pu.publicacion.id IN :idsPublicacion
            ORDER BY pu.id
            """)
    List<PublicacionUsuario> findByPublicacionIdIn(@Param("idsPublicacion") Collection<Integer> idsPublicacion);
}
