package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Convocatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer> {
    List<Convocatoria> findBySemilleroId(Integer semilleroId);
    List<Convocatoria> findByEstadoId(Integer estadoId);
    
    // Consultas para Servicio 2: Gestor de Convocatorias
    // Convocatorias activas con cupos disponibles
    List<Convocatoria> findByCuposDisponiblesGreaterThan(Integer cupos);
    
    // Convocatorias que han pasado su fecha de cierre
    List<Convocatoria> findByFechaCierreBefore(LocalDate fecha);
    
    // Convocatorias activas con cupos disponibles
    @Query("SELECT c FROM Convocatoria c WHERE c.cuposDisponibles > 0 AND c.estado.id = ?1")
    List<Convocatoria> findActivasConCupos(Integer estadoId);
    
    // Convocatorias que necesitan ser cerradas (fecha expirada)
    @Query("SELECT c FROM Convocatoria c WHERE c.fechaCierre < CURRENT_DATE AND c.estado.nombre != 'Cerrada'")
    List<Convocatoria> findConvocatoriasExpiradas();
}
