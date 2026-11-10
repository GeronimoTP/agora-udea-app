package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.EstudianteSemillero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteSemilleroRepository extends JpaRepository<EstudianteSemillero, Integer> {
    List<EstudianteSemillero> findBySemilleroId(Integer semilleroId);
    List<EstudianteSemillero> findByEstudianteId(Integer estudianteId);
    Optional<EstudianteSemillero> findBySemilleroIdAndEstudianteId(Integer semilleroId, Integer estudianteId);

    // Integrante activo = vinculación sin fecha de salida
    List<EstudianteSemillero> findBySemilleroIdAndFechaSalidaIsNull(Integer semilleroId);
    List<EstudianteSemillero> findByEstudianteIdAndFechaSalidaIsNull(Integer estudianteId);
    boolean existsBySemilleroIdAndEstudianteIdAndFechaSalidaIsNull(Integer semilleroId, Integer estudianteId);

    @Query("""
            SELECT es.semillero.id AS idSemillero, COUNT(es) AS total
            FROM EstudianteSemillero es
            WHERE es.semillero.id IN :idsSemillero AND es.fechaSalida IS NULL
            GROUP BY es.semillero.id
            """)
    List<ConteoPorSemillero> contarIntegrantesActivosPorSemillero(@Param("idsSemillero") Collection<Integer> idsSemillero);
}
