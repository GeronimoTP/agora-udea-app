package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.PostulacionHabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostulacionHabilidadRepository extends JpaRepository<PostulacionHabilidad, Integer> {

    // Método básico para buscar habilidades por el ID de la convocatoria
    @Query("SELECT ph FROM PostulacionHabilidad ph WHERE ph.postulacion.convocatoria.id = :convocatoriaId")
    List<PostulacionHabilidad> findByConvocatoriaId(@Param("convocatoriaId") Integer convocatoriaId);

    // Todas las habilidades declaradas en CUALQUIER postulación recibida por un semillero
    // (une por semillero directamente, evitando el loop N+1 por convocatoria)
    @Query("SELECT ph FROM PostulacionHabilidad ph WHERE ph.postulacion.convocatoria.semillero.id = :semilleroId")
    List<PostulacionHabilidad> findBySemilleroId(@Param("semilleroId") Integer semilleroId);

    // Habilidades declaradas SOLO en postulaciones de un semillero cuyo estado tiene un nombre dado
    // (ej. "Aceptada"): la señal más confiable de qué habilidades realmente valora el semillero
    @Query("SELECT ph FROM PostulacionHabilidad ph " +
           "WHERE ph.postulacion.convocatoria.semillero.id = :semilleroId " +
           "AND ph.postulacion.estado.nombre = :estadoNombre")
    List<PostulacionHabilidad> findBySemilleroIdAndEstadoNombre(
            @Param("semilleroId") Integer semilleroId,
            @Param("estadoNombre") String estadoNombre);

    // Consulta OPTIMIZADA: Navega por las entidades (Postulacion -> Estudiante)
    // y extrae solo los IDs de las habilidades para un estudiante específico.
    @Query("SELECT ph.habilidad.id FROM PostulacionHabilidad ph WHERE ph.postulacion.estudiante.id = :estudianteId")
    List<Integer> findHabilidadIdsByEstudianteId(@Param("estudianteId") Integer estudianteId);

    // Habilidades declaradas en una postulación específica
    List<PostulacionHabilidad> findByPostulacionId(Integer postulacionId);

    Optional<PostulacionHabilidad> findByPostulacionIdAndHabilidadId(Integer postulacionId, Integer habilidadId);


    /** Habilidades de varias postulaciones (con su nombre), en el orden en que se declararon. */
    @Query("""
            SELECT ph FROM PostulacionHabilidad ph
            JOIN FETCH ph.habilidad
            WHERE ph.postulacion.id IN :idsPostulacion
            ORDER BY ph.id
            """)
    List<PostulacionHabilidad> findByPostulacionIdIn(@Param("idsPostulacion") Collection<Integer> idsPostulacion);

    /** Habilidad declarada en una postulación recibida por un semillero, con el estado de esa postulación. */
    interface HabilidadDeSemillero {
        Integer getIdSemillero();
        Integer getIdHabilidad();
        String getEstadoPostulacion();
    }

    // Historial de habilidades de varios semilleros en una sola consulta: alimenta el factor de
    // habilidades del motor de recomendación
    @Query("""
            SELECT c.semillero.id AS idSemillero, ph.habilidad.id AS idHabilidad, e.nombre AS estadoPostulacion
            FROM PostulacionHabilidad ph
            JOIN ph.postulacion p
            JOIN p.convocatoria c
            LEFT JOIN p.estado e
            WHERE c.semillero.id IN :idsSemillero
            """)
    List<HabilidadDeSemillero> findHabilidadesDeSemilleros(@Param("idsSemillero") Collection<Integer> idsSemillero);
}
