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
    List<Postulacion> findByConvocatoriaSemilleroId(Integer semilleroId);
    
    // Consultas para Servicio 2: Gestor de Convocatorias y Resoluciones en Cascada
    // Postulaciones activas de un estudiante (para cascada)
    List<Postulacion> findByEstudianteIdAndEstadoId(Integer estudianteId, Integer estadoId);
    
    // Postulaciones de un estudiante con estado específico
    @Query("SELECT p FROM Postulacion p WHERE p.estudiante.id = ?1 AND p.estado.nombre IN (?2)")
    List<Postulacion> findPostulacionesActivasByEstudiante(Integer estudianteId, String... estadoNombres);
    
    // Postulaciones de una convocatoria con estado específico
    @Query("SELECT p FROM Postulacion p WHERE p.convocatoria.id = ?1 AND p.estado.nombre IN (?2)")
    List<Postulacion> findPostulacionesByConvocatoriaAndEstado(Integer convocatoriaId, String... estadoNombres);

    // Postulaciones sin resolver de convocatorias que ya están cerradas (por cupos, cierre manual o fecha)
    @Query("SELECT p FROM Postulacion p WHERE p.convocatoria.estado.nombre = ?1 AND p.estado.nombre IN (?2)")
    List<Postulacion> findPostulacionesEnConvocatoriasConEstado(String estadoConvocatoria, String... estadoNombres);
    
    // Ofertas (Pre-aprobada) que siguen vigentes en una convocatoria: no pueden superar los cupos disponibles
    @Query("SELECT COUNT(p) FROM Postulacion p WHERE p.convocatoria.id = ?1 AND p.estado.nombre = ?2 AND p.fechaPreAprobacion >= ?3")
    long contarOfertasVigentes(Integer convocatoriaId, String estadoPreAprobada, java.time.ZonedDateTime desde);

    // Ofertas cuyo plazo de respuesta venció (emitidas antes de `limite`)
    @Query("SELECT p FROM Postulacion p WHERE p.estado.nombre = ?1 AND p.fechaPreAprobacion < ?2")
    List<Postulacion> findOfertasVencidas(String estadoPreAprobada, java.time.ZonedDateTime limite);

    @Query("SELECT p FROM Postulacion p WHERE p.convocatoria.id = ?1 AND p.estado.nombre = ?2 AND p.fechaPreAprobacion < ?3")
    List<Postulacion> findOfertasVencidasDeConvocatoria(Integer convocatoriaId, String estadoPreAprobada, java.time.ZonedDateTime limite);

    // Búsqueda única: Estudiante en Convocatoria
    Optional<Postulacion> findByConvocatoriaIdAndEstudianteId(Integer convocatoriaId, Integer estudianteId);
    
    // Postulaciones sin respuesta del estudiante (para expiración automática)
    @Query("SELECT p FROM Postulacion p WHERE p.fechaDecisionEstudiante IS NULL AND p.convocatoria.fechaCierre < CURRENT_DATE")
    List<Postulacion> findPostulacionesSinRespuestaExpiradas();
}
