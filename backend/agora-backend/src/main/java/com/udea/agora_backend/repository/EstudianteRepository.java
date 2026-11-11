package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer>{
    Optional<Estudiante> findByUsuarioId(Integer usuarioId);

    /** IDs de los usuarios que tienen perfil de estudiante. */
    @Query("SELECT e.usuario.id FROM Estudiante e")
    List<Integer> findIdsUsuario();


    /** De los usuarios dados, los que tienen perfil de estudiante. */
    @Query("SELECT e.usuario.id FROM Estudiante e WHERE e.usuario.id IN :idsUsuario")
    List<Integer> findIdsUsuarioEntre(@Param("idsUsuario") Collection<Integer> idsUsuario);
}
