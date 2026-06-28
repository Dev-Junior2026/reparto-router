package com.luispacheco.reparto.algorithm;

import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.model.Ruta;
import com.luispacheco.reparto.service.DistanciaService;

import java.util.List;

public class AlgoritmoDosPopt {

    private DistanciaService distanciaService;

    public AlgoritmoDosPopt() {
        this.distanciaService = new DistanciaService();
    }

    /**
     * Mejora una ruta usando el algoritmo 2-opt.
     * Intenta intercambiar pares de aristas para reducir la distancia total.
     */
    public Ruta mejorar(Ruta ruta) {
        List<Parada> paradas = ruta.getParadasOrdenadas();

        // El almacén (primera y última parada) no se mueven
        if (paradas.size() <= 3) {
            return ruta;  // No hay suficientes paradas para mejorar
        }

        boolean mejora = true;
        int iteraciones = 0;
        int maxIteraciones = 100;  // Evitar bucle infinito

        while (mejora && iteraciones < maxIteraciones) {
            mejora = false;
            iteraciones++;

            // Probar intercambios entre pares de aristas
            for (int i = 1; i < paradas.size() - 2; i++) {
                for (int k = i + 1; k < paradas.size() - 1; k++) {
                    // Calcular distancia actual
                    double distanciaActual = calcularDistanciaSegmento(paradas, i - 1, i, k, k + 1);

                    // Invertir segmento entre i y k
                    invertirSegmento(paradas, i, k);

                    // Calcular distancia después del intercambio
                    double distanciaNueva = calcularDistanciaSegmento(paradas, i - 1, i, k, k + 1);

                    if (distanciaNueva < distanciaActual) {
                        // Mejora encontrada, mantener el cambio
                        mejora = true;
                    } else {
                        // Sin mejora, revertir
                        invertirSegmento(paradas, i, k);
                    }
                }
            }
        }

        // Recalcular distancia total de la ruta mejorada
        double distanciaTotal = 0.0;
        for (int i = 0; i < paradas.size() - 1; i++) {
            Parada p1 = paradas.get(i);
            Parada p2 = paradas.get(i + 1);
            distanciaTotal += distanciaService.calcularDistanciaKm(
                    p1.getLatitud(), p1.getLongitud(),
                    p2.getLatitud(), p2.getLongitud()
            );
        }

        ruta.setDistanciaTotalKm(distanciaTotal);
        return ruta;
    }

    /**
     * Calcula la distancia de 4 segmentos: (a→b) + (b→c) + (c→d) + (d→e)
     * donde se va a invertir el segmento b→...→d
     */
    private double calcularDistanciaSegmento(List<Parada> paradas, int a, int b, int d, int e) {
        double dist = 0.0;

        if (a >= 0) {
            dist += distanciaService.calcularDistanciaKm(
                    paradas.get(a).getLatitud(), paradas.get(a).getLongitud(),
                    paradas.get(b).getLatitud(), paradas.get(b).getLongitud()
            );
        }

        dist += distanciaService.calcularDistanciaKm(
                paradas.get(d).getLatitud(), paradas.get(d).getLongitud(),
                paradas.get(e).getLatitud(), paradas.get(e).getLongitud()
        );

        return dist;
    }

    /**
     * Invierte el orden de paradas entre los índices i y k (inclusive)
     */
    private void invertirSegmento(List<Parada> paradas, int i, int k) {
        while (i < k) {
            Parada temp = paradas.get(i);
            paradas.set(i, paradas.get(k));
            paradas.set(k, temp);
            i++;
            k--;
        }
    }
}