package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Semillero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemilleroRepository extends JpaRepository<Semillero, Integer>{

    Optional<Semillero> findByCodigoIdentificador(String codigoIdentificador);

    List<Semillero> findByProgramaId(Integer programaId);
    List<Semillero> findByEstadoId(Integer estadoId);

    @Query("SELECT DISTINCT s.estudianteLider.id FROM Semillero s")
    List<Integer> findIdsEstudiantesLideres();

    @Query("SELECT s.id FROM Semillero s WHERE s.estudianteLider.id = :idEstudiante")
    List<Integer> findIdsByEstudianteLiderId(@Param("idEstudiante") Integer idEstudiante);

    /** Semilleros en un estado de la categoría "Estado Semillero" (p. ej. los pendientes de aprobación). */
    @Query("""
            SELECT s FROM Semillero s
            WHERE s.estado.categoriaEstado.nombre = 'Estado Semillero' AND s.estado.nombre = :estado
            ORDER BY s.fechaSolicitud, s.id
            """)
    List<Semillero> findByNombreEstado(@Param("estado") String estado);
}
