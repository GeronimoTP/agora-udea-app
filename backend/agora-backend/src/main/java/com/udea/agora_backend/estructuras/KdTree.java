package com.udea.agora_backend.estructuras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;

/**
 * Árbol KD (k-dimensional tree): árbol binario de búsqueda sobre puntos de k dimensiones.
 *
 * <p>Cada nivel del árbol divide el espacio con un hiperplano perpendicular a un eje. El eje
 * se elige de forma cíclica según la profundidad (nivel 0 → eje 0, nivel 1 → eje 1, …,
 * nivel k → eje 0 de nuevo). En cada nodo, los puntos del subárbol izquierdo tienen en ese
 * eje una coordenada menor o igual a la del nodo, y los del derecho una mayor o igual.
 *
 * <p>Construcción: en cada nivel se toma como nodo la <b>mediana</b> de los puntos según el
 * eje del nivel, de modo que el árbol queda balanceado (profundidad ⌈log₂(n+1)⌉). La mediana
 * se obtiene con <i>quickselect</i> (O(n) promedio por nivel), así que construir el árbol
 * cuesta O(n log n).
 *
 * <p>Búsqueda de los k vecinos más cercanos (k-NN): se baja primero por la rama donde cae
 * el punto de consulta y, al volver, solo se explora la otra rama si el hiperplano de corte
 * está más cerca que el peor vecino encontrado hasta ahora (<b>poda</b>). Con pocas
 * dimensiones esto revisa O(log n) nodos en promedio, en lugar de los n de un recorrido
 * lineal. Con muchas dimensiones la poda deja de funcionar (maldición de la dimensionalidad)
 * y la búsqueda se acerca a O(n): el árbol es útil cuando n ≫ 2^dimensiones.
 *
 * <p>El árbol es inmutable: para reflejar cambios en los datos se construye uno nuevo.
 *
 * @param <T> dato asociado a cada punto (por ejemplo, el ID de un semillero)
 */
public final class KdTree<T> {

    /** Punto que se inserta en el árbol junto con su dato asociado. */
    public record Entrada<T>(double[] punto, T valor) {
    }

    /** Vecino encontrado por la búsqueda, con su distancia euclidiana al cuadrado. */
    public record Vecino<T>(T valor, double[] punto, double distanciaCuadrada) {
        public double distancia() {
            return Math.sqrt(distanciaCuadrada);
        }
    }

    /**
     * Resultado de una búsqueda k-NN: vecinos ordenados del más cercano al más lejano y
     * cuántos nodos tuvo que visitar el algoritmo (sirve para observar la poda).
     */
    public record ResultadoBusqueda<T>(List<Vecino<T>> vecinos, int nodosVisitados) {
    }

    private static final class Nodo<T> {
        private final Entrada<T> entrada;
        private final int eje;
        private Nodo<T> izquierdo;
        private Nodo<T> derecho;

        private Nodo(Entrada<T> entrada, int eje) {
            this.entrada = entrada;
            this.eje = eje;
        }
    }

    private final int dimensiones;
    private final int tamano;
    private final int profundidad;
    private final Nodo<T> raiz;

    private KdTree(int dimensiones, int tamano, int profundidad, Nodo<T> raiz) {
        this.dimensiones = dimensiones;
        this.tamano = tamano;
        this.profundidad = profundidad;
        this.raiz = raiz;
    }

    /**
     * Construye un árbol balanceado con todos los puntos dados.
     *
     * @param dimensiones número de coordenadas de cada punto (k)
     * @param entradas    puntos a indexar; la lista no se modifica
     */
    public static <T> KdTree<T> construir(int dimensiones, List<Entrada<T>> entradas) {
        if (dimensiones < 1) {
            throw new IllegalArgumentException("El árbol necesita al menos una dimensión");
        }
        for (Entrada<T> entrada : entradas) {
            validarDimension(entrada.punto(), dimensiones);
        }
        @SuppressWarnings("unchecked")
        Entrada<T>[] puntos = entradas.toArray(new Entrada[0]);
        int[] profundidadMaxima = {0};
        Nodo<T> raiz = construirSubarbol(puntos, 0, puntos.length, 0, dimensiones, profundidadMaxima);
        return new KdTree<>(dimensiones, puntos.length, profundidadMaxima[0], raiz);
    }

    /**
     * Construye recursivamente el subárbol con los puntos del rango [inicio, fin).
     * El punto mediano según el eje del nivel queda como raíz del subárbol; los menores a
     * su izquierda y los mayores a su derecha.
     */
    private static <T> Nodo<T> construirSubarbol(Entrada<T>[] puntos, int inicio, int fin, int nivel,
                                                 int dimensiones, int[] profundidadMaxima) {
        if (inicio >= fin) {
            return null;
        }
        profundidadMaxima[0] = Math.max(profundidadMaxima[0], nivel + 1);
        int eje = nivel % dimensiones;
        int mediana = (inicio + fin) >>> 1;
        seleccionar(puntos, inicio, fin - 1, mediana, eje);

        Nodo<T> nodo = new Nodo<>(puntos[mediana], eje);
        nodo.izquierdo = construirSubarbol(puntos, inicio, mediana, nivel + 1, dimensiones, profundidadMaxima);
        nodo.derecho = construirSubarbol(puntos, mediana + 1, fin, nivel + 1, dimensiones, profundidadMaxima);
        return nodo;
    }

    /**
     * Quickselect con partición en tres vías: reordena puntos[izquierda..derecha] para que la
     * posición {@code objetivo} quede con el valor que tendría si el rango estuviera ordenado
     * por el eje dado, con los menores o iguales antes y los mayores o iguales después.
     * La partición en tres vías evita el peor caso cuando hay muchas coordenadas repetidas,
     * algo habitual aquí (muchos semilleros tienen 0 en la mayoría de áreas).
     */
    private static <T> void seleccionar(Entrada<T>[] puntos, int izquierda, int derecha, int objetivo, int eje) {
        while (izquierda < derecha) {
            double pivote = puntos[(izquierda + derecha) >>> 1].punto()[eje];
            int menores = izquierda;
            int actual = izquierda;
            int mayores = derecha;
            // Invariante: [izquierda, menores) < pivote, [menores, actual) == pivote, (mayores, derecha] > pivote
            while (actual <= mayores) {
                double valor = puntos[actual].punto()[eje];
                if (valor < pivote) {
                    intercambiar(puntos, menores++, actual++);
                } else if (valor > pivote) {
                    intercambiar(puntos, actual, mayores--);
                } else {
                    actual++;
                }
            }
            if (objetivo < menores) {
                derecha = menores - 1;
            } else if (objetivo > mayores) {
                izquierda = mayores + 1;
            } else {
                return; // El objetivo cae entre los iguales al pivote: ya está en su lugar
            }
        }
    }

    private static <T> void intercambiar(Entrada<T>[] puntos, int i, int j) {
        Entrada<T> temporal = puntos[i];
        puntos[i] = puntos[j];
        puntos[j] = temporal;
    }

    /**
     * Busca los {@code k} puntos más cercanos (distancia euclidiana) al punto de consulta.
     *
     * @param consulta punto de referencia
     * @param k        cantidad máxima de vecinos a devolver
     * @param admitido filtro sobre el dato asociado: los puntos no admitidos se recorren
     *                 (siguen sirviendo para navegar el árbol) pero no se devuelven
     */
    public ResultadoBusqueda<T> buscarVecinosMasCercanos(double[] consulta, int k, Predicate<? super T> admitido) {
        validarDimension(consulta, dimensiones);
        if (k < 1) {
            throw new IllegalArgumentException("k debe ser mayor que cero");
        }
        // Montículo de máximos: en la cima está el peor de los k mejores vecinos encontrados
        PriorityQueue<Vecino<T>> mejores = new PriorityQueue<>(
                Comparator.comparingDouble((Vecino<T> v) -> v.distanciaCuadrada()).reversed());
        int[] nodosVisitados = {0};
        buscar(raiz, consulta, k, admitido, mejores, nodosVisitados);

        List<Vecino<T>> vecinos = new ArrayList<>(mejores);
        vecinos.sort(Comparator.comparingDouble(Vecino::distanciaCuadrada));
        return new ResultadoBusqueda<>(vecinos, nodosVisitados[0]);
    }

    private void buscar(Nodo<T> nodo, double[] consulta, int k, Predicate<? super T> admitido,
                        PriorityQueue<Vecino<T>> mejores, int[] nodosVisitados) {
        if (nodo == null) {
            return;
        }
        nodosVisitados[0]++;

        double[] punto = nodo.entrada.punto();
        if (admitido.test(nodo.entrada.valor())) {
            double distancia = distanciaCuadrada(consulta, punto);
            if (mejores.size() < k) {
                mejores.add(new Vecino<>(nodo.entrada.valor(), punto, distancia));
            } else if (distancia < mejores.peek().distanciaCuadrada()) {
                mejores.poll();
                mejores.add(new Vecino<>(nodo.entrada.valor(), punto, distancia));
            }
        }

        // Distancia (con signo) de la consulta al hiperplano de corte de este nodo
        double diferencia = consulta[nodo.eje] - punto[nodo.eje];
        Nodo<T> cercano = diferencia < 0 ? nodo.izquierdo : nodo.derecho;
        Nodo<T> lejano = diferencia < 0 ? nodo.derecho : nodo.izquierdo;

        // 1. Primero el lado del hiperplano donde está la consulta: ahí suelen estar los vecinos
        buscar(cercano, consulta, k, admitido, mejores, nodosVisitados);

        // 2. Poda: cualquier punto del otro lado está al menos a |diferencia| de la consulta.
        //    Si ya hay k vecinos y el peor está más cerca que el hiperplano, esa rama no puede
        //    mejorar el resultado y no se visita.
        if (mejores.size() < k || diferencia * diferencia < mejores.peek().distanciaCuadrada()) {
            buscar(lejano, consulta, k, admitido, mejores, nodosVisitados);
        }
    }

    public static double distanciaCuadrada(double[] a, double[] b) {
        double suma = 0;
        for (int i = 0; i < a.length; i++) {
            double diferencia = a[i] - b[i];
            suma += diferencia * diferencia;
        }
        return suma;
    }

    private static void validarDimension(double[] punto, int dimensiones) {
        if (punto == null || punto.length != dimensiones) {
            throw new IllegalArgumentException("Se esperaba un punto de " + dimensiones + " dimensiones");
        }
    }

    public int dimensiones() {
        return dimensiones;
    }

    public int tamano() {
        return tamano;
    }

    public int profundidad() {
        return profundidad;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }
}
