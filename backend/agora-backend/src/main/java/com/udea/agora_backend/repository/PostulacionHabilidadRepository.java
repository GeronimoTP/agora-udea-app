package com.udea.agora_backend.repository;
import com.udea.agora_backend.model.PostulacionHabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostulacionHabilidadRepository extends JpaRepository<PostulacionHabilidad, Integer> {
    
    // Método básico para buscar habilidades por el ID de la convocatoria
    @Query("SELECT ph FROM PostulacionHabilidad ph WHERE ph.postulacion.convocatoria.id = :convocatoriaId")
    List<PostulacionHabilidad> findByConvocatoriaId(@Param("convocatoriaId") Integer convocatoriaId);

    // Consulta OPTIMIZADA: Navega por las entidades (Postulacion -> Estudiante) 
    // y extrae solo los IDs de las habilidades para un estudiante específico.
    @Query("SELECT ph.habilidad.id FROM PostulacionHabilidad ph WHERE ph.postulacion.estudiante.id = :estudianteId")
    List<Integer> findHabilidadIdsByEstudianteId(@Param("estudianteId") Integer estudianteId);
    
}