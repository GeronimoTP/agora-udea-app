package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostulacionRepository extends JpaRepository<Postulacion, Integer> {
    List<Postulacion> findByConvocatoriaId(Integer convocatoriaId);
    List<Postulacion> findByEstudianteId(Integer estudianteId);
    
    // Consultas para Servicio 2: Gestor de Convocatorias y Resoluciones en Cascada
    // Postulaciones activas de un estudiante (para cascada)
    List<Postulacion> findByEstudianteIdAndEstadoId(Integer estudianteId, Integer estadoId);
    
    // Postulaciones de un estudiante con estado específico
    @Query("SELECT p FROM Postulacion p WHERE p.estudiante.id = ?1 AND p.estado.nombre IN (?2)")
    List<Postulacion> findPostulacionesActivasByEstudiante(Integer estudianteId, String... estadoNombres);
    
    // Postulaciones de una convocatoria con estado específico
    @Query("SELECT p FROM Postulacion p WHERE p.convocatoria.id = ?1 AND p.estado.nombre IN (?2)")
    List<Postulacion> findPostulacionesByConvocatoriaAndEstado(Integer convocatoriaId, String... estadoNombres);
    
    // Búsqueda única: Estudiante en Convocatoria
    Optional<Postulacion> findByConvocatoriaIdAndEstudianteId(Integer convocatoriaId, Integer estudianteId);
    
    // Postulaciones sin respuesta del estudiante (para expiración automática)
    @Query("SELECT p FROM Postulacion p WHERE p.fechaDecisionEstudiante IS NULL AND p.convocatoria.fechaCierre < CURRENT_DATE")
    List<Postulacion> findPostulacionesSinRespuestaExpiradas();
}
