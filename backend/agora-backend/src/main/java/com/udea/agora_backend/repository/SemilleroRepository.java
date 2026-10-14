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

    @Query("SELECT s.id FROM Semillero s WHERE s.estudianteLider.id = :idEstudiante")
    List<Integer> findIdsByEstudianteLiderId(@Param("idEstudiante") Integer idEstudiante);
    
}
