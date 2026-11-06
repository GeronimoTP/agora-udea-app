package com.udea.agora_backend.estructuras;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del árbol KD contra una búsqueda por fuerza bruta (recorrido lineal), que es
 * trivialmente correcta: ambos deben encontrar los mismos k vecinos más cercanos.
 */
class KdTreeTest {

    @Test
    void ejemploDeClaseEnDosDimensiones() {
        // Ejemplo clásico de árbol KD en 2D
        List<KdTree.Entrada<String>> puntos = List.of(
                new KdTree.Entrada<>(new double[]{2, 3}, "A"),
                new KdTree.Entrada<>(new double[]{5, 4}, "B"),
                new KdTree.Entrada<>(new double[]{9, 6}, "C"),
                new KdTree.Entrada<>(new double[]{4, 7}, "D"),
                new KdTree.Entrada<>(new double[]{8, 1}, "E"),
                new KdTree.Entrada<>(new double[]{7, 2}, "F"));
        KdTree<String> arbol = KdTree.construir(2, puntos);

        assertEquals(6, arbol.tamano());
        assertEquals(3, arbol.profundidad()); // Balanceado: ⌈log₂(6+1)⌉ = 3

        KdTree.ResultadoBusqueda<String> resultado =
                arbol.buscarVecinosMasCercanos(new double[]{9, 2}, 2, valor -> true);
        assertEquals(List.of("E", "F"), resultado.vecinos().stream().map(KdTree.Vecino::valor).toList());
        assertEquals(2.0, resultado.vecinos().get(0).distanciaCuadrada(), 1e-9); // (9-8)² + (2-1)²
    }

    @Test
    void coincideConFuerzaBrutaEnDatosAleatorios() {
        Random azar = new Random(42);
        for (int dimensiones = 1; dimensiones <= 6; dimensiones++) {
            for (int n : new int[]{1, 2, 10, 100, 500}) {
                List<KdTree.Entrada<Integer>> puntos = puntosAleatorios(azar, n, dimensiones, false);
                KdTree<Integer> arbol = KdTree.construir(dimensiones, puntos);
                for (int consulta = 0; consulta < 20; consulta++) {
                    double[] q = puntoAleatorio(azar, dimensiones, false);
                    int k = 1 + azar.nextInt(8);
                    compararConFuerzaBruta(arbol, puntos, q, k, valor -> true);
                }
            }
        }
    }

    @Test
    void coincideConFuerzaBrutaConCoordenadasRepetidasYFiltro() {
        // Coordenadas enteras pequeñas: muchos empates, como los vectores de áreas de los semilleros
        Random azar = new Random(7);
        List<KdTree.Entrada<Integer>> puntos = puntosAleatorios(azar, 300, 4, true);
        KdTree<Integer> arbol = KdTree.construir(4, puntos);
        Predicate<Integer> soloPares = valor -> valor % 2 == 0;
        for (int consulta = 0; consulta < 50; consulta++) {
            compararConFuerzaBruta(arbol, puntos, puntoAleatorio(azar, 4, true), 5, soloPares);
        }
    }

    @Test
    void laPodaEvitaRecorrerTodoElArbolEnPocasDimensiones() {
        Random azar = new Random(1);
        List<KdTree.Entrada<Integer>> puntos = puntosAleatorios(azar, 10_000, 2, false);
        KdTree<Integer> arbol = KdTree.construir(2, puntos);

        KdTree.ResultadoBusqueda<Integer> resultado =
                arbol.buscarVecinosMasCercanos(new double[]{0.5, 0.5}, 5, valor -> true);

        assertEquals(5, resultado.vecinos().size());
        assertTrue(resultado.nodosVisitados() < 200,
                "Con 2 dimensiones la búsqueda debe visitar pocos nodos, visitó " + resultado.nodosVisitados());
    }

    @Test
    void arbolVacioYValidaciones() {
        KdTree<Integer> vacio = KdTree.construir(3, List.of());
        assertTrue(vacio.estaVacio());
        assertTrue(vacio.buscarVecinosMasCercanos(new double[]{0, 0, 0}, 3, v -> true).vecinos().isEmpty());

        assertThrows(IllegalArgumentException.class,
                () -> vacio.buscarVecinosMasCercanos(new double[]{0, 0}, 3, v -> true));
        assertThrows(IllegalArgumentException.class,
                () -> KdTree.construir(2, List.of(new KdTree.Entrada<>(new double[]{1}, 1))));
    }

    private static void compararConFuerzaBruta(KdTree<Integer> arbol, List<KdTree.Entrada<Integer>> puntos,
                                               double[] consulta, int k, Predicate<Integer> admitido) {
        double[] esperadas = puntos.stream()
                .filter(p -> admitido.test(p.valor()))
                .map(p -> KdTree.distanciaCuadrada(consulta, p.punto()))
                .sorted(Comparator.naturalOrder())
                .limit(k)
                .mapToDouble(Double::doubleValue)
                .toArray();

        List<KdTree.Vecino<Integer>> vecinos = arbol.buscarVecinosMasCercanos(consulta, k, admitido).vecinos();
        // Se comparan distancias y no IDs: con empates, varios puntos son igual de válidos
        double[] obtenidas = vecinos.stream().mapToDouble(KdTree.Vecino::distanciaCuadrada).toArray();
        assertArrayEquals(esperadas, obtenidas, 1e-12);
        assertTrue(vecinos.stream().allMatch(v -> admitido.test(v.valor())));
    }

    private static List<KdTree.Entrada<Integer>> puntosAleatorios(Random azar, int n, int dimensiones,
                                                                  boolean enteros) {
        List<KdTree.Entrada<Integer>> puntos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            puntos.add(new KdTree.Entrada<>(puntoAleatorio(azar, dimensiones, enteros), i));
        }
        return puntos;
    }

    private static double[] puntoAleatorio(Random azar, int dimensiones, boolean enteros) {
        double[] punto = new double[dimensiones];
        for (int i = 0; i < dimensiones; i++) {
            punto[i] = enteros ? azar.nextInt(3) : azar.nextDouble();
        }
        return punto;
    }
}
