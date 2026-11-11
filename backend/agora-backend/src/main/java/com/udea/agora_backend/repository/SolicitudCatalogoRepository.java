package com.udea.agora_backend.repository;

import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import com.udea.agora_backend.model.SolicitudCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudCatalogoRepository extends JpaRepository<SolicitudCatalogo, Integer> {

    List<SolicitudCatalogo> findBySolicitanteIdOrderByFechaSolicitudDesc(Integer idUsuario);

    /** Por estado; las pendientes más antiguas primero, para atenderlas en orden de llegada. */
    @Query("SELECT s FROM SolicitudCatalogo s WHERE s.estado.nombre = :estado ORDER BY s.fechaSolicitud")
    List<SolicitudCatalogo> findByNombreEstado(@Param("estado") String estado);

    @Query("SELECT s FROM SolicitudCatalogo s ORDER BY s.fechaSolicitud DESC")
    List<SolicitudCatalogo> findTodasRecientesPrimero();

    @Query("SELECT COUNT(s) FROM SolicitudCatalogo s WHERE s.estado.nombre = :estado")
    long contarPorEstado(@Param("estado") String estado);

    @Query("""
            SELECT COUNT(s) > 0 FROM SolicitudCatalogo s
            WHERE s.tipo = :tipo AND LOWER(s.nombre) = LOWER(:nombre) AND s.estado.nombre = :estado
            """)
    boolean existeConNombre(@Param("tipo") TipoElementoCatalogo tipo, @Param("nombre") String nombre,
                            @Param("estado") String estado);
}
