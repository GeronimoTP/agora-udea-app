package com.udea.agora_backend.repository;

/**
 * Proyección de las consultas agregadas por semillero ({@code GROUP BY id_semillero}). Permite
 * calcular los totales de varios semilleros en una sola consulta en lugar de una por semillero.
 */
public interface ConteoPorSemillero {
    Integer getIdSemillero();

    Long getTotal();
}
