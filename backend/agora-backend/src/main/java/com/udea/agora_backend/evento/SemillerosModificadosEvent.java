package com.udea.agora_backend.evento;

/**
 * Se publica cuando cambia algo que afecta la posición de los semilleros en el árbol KD de
 * semilleros similares: un semillero creado, editado, eliminado o que cambió de estado,
 * sus líneas de investigación, o el catálogo de líneas y áreas de especialidad.
 * El índice escucha el evento después del commit y se reconstruye en la siguiente consulta.
 */
public record SemillerosModificadosEvent() {
}
