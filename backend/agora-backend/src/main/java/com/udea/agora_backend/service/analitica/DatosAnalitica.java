package com.udea.agora_backend.service.analitica;

import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.PostulacionHabilidad;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.Publicacion;
import com.udea.agora_backend.model.PublicacionUsuario;
import com.udea.agora_backend.model.ReconocimientoSemillero;

import java.util.List;
import java.util.Set;

/**
 * Datos sobre los que se calculan los indicadores. Su alcance lo decide quien los carga:
 * toda la universidad (vista del administrador) o un solo semillero (vista de sus líderes).
 * Los cálculos de {@link CalculadoraIndicadores} no saben de qué alcance se trata.
 *
 * @param idsUsuariosEstudiantes usuarios con perfil de estudiante, para distinguir autorías
 */
public record DatosAnalitica(
        List<Convocatoria> convocatorias,
        List<Postulacion> postulaciones,
        List<PostulacionHabilidad> habilidadesPostuladas,
        List<Proyecto> proyectos,
        List<Publicacion> publicaciones,
        List<PublicacionUsuario> autorias,
        List<ReconocimientoSemillero> reconocimientos,
        List<EstudianteSemillero> vinculaciones,
        Set<Integer> idsUsuariosEstudiantes) {
}
