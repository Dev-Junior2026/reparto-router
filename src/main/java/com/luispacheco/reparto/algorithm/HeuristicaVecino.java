package com.luispacheco.reparto.algorithm;

import com.luispacheco.reparto.model.ConfiguracionReparto;
import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.model.Ruta;
import com.luispacheco.reparto.service.DistanciaService;
import com.luispacheco.reparto.service.HorarioService;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

public class HeuristicaVecino {

    private static final double VELOCIDAD_MEDIA_KMH = 40.0;

    private DistanciaService distanciaService;
    private HorarioService horarioService;

    public HeuristicaVecino() {
        this.distanciaService = new DistanciaService();
        this.horarioService = new HorarioService();
    }

    public Ruta calcular(List<Parada> paradas, ConfiguracionReparto config) {
        // aqui vendra todo el algoritmo

        Parada almacen = null;
        for (Parada parada : paradas) {
            if (parada.isEsAlmacen()) {
                almacen = parada;
            }
        }

        List<Parada> pendientes = new ArrayList<>(paradas);
        pendientes.remove(almacen);

        Ruta ruta = new Ruta(config.getHoraInicioJornada());
        ruta.agregarParada(almacen);

        LocalTime horaActual = config.getHoraInicioJornada();
        Parada posicionActual = almacen;

        while (!pendientes.isEmpty()) {

            Parada mejorParada = null;
            double mejorDistancia = Double.MAX_VALUE;
            LocalTime mejorHoraLlegada = null;

            // aqui ira el bucle interno (Paso 3b)

            for (Parada candidata : pendientes) {

                double distancia = distanciaService.calcularDistanciaKm(
                        posicionActual.getLatitud(), posicionActual.getLongitud(),
                        candidata.getLatitud(), candidata.getLongitud()
                );

                double tiempoViajeMinutos = (distancia / VELOCIDAD_MEDIA_KMH) * 60;
                LocalTime horaLlegadaCandidata = horaActual.plusMinutes((long) tiempoViajeMinutos);

                if (horarioService.esAlcanzable(horaLlegadaCandidata, candidata)) {
                    if (distancia < mejorDistancia) {
                        mejorDistancia = distancia;
                        mejorParada = candidata;
                        mejorHoraLlegada = horaLlegadaCandidata;
                    }
                }
            }

            if (mejorParada == null) {
                for (Parada candidata : pendientes) {

                    double distancia = distanciaService.calcularDistanciaKm(
                            posicionActual.getLatitud(), posicionActual.getLongitud(),
                            candidata.getLatitud(), candidata.getLongitud()
                    );

                    double tiempoViajeMinutos = (distancia / VELOCIDAD_MEDIA_KMH) * 60;
                    LocalTime horaLlegadaCandidata = horaActual.plusMinutes((long) tiempoViajeMinutos);

                    if (distancia < mejorDistancia) {
                        mejorDistancia = distancia;
                        mejorParada = candidata;
                        mejorHoraLlegada = horaLlegadaCandidata;
                    }
                }
            }

            // Paso 4: actualizaciones tras elegir la mejor parada
            horaActual = horarioService.calcularHoraLlegadaConEspera(mejorHoraLlegada, mejorParada);
            horaActual = horaActual.plusMinutes(mejorParada.getTiempoDescargaMin());

            mejorParada.setHoraLlegadaEstimada(horaActual);

            ruta.agregarParada(mejorParada);
            ruta.setDistanciaTotalKm(ruta.getDistanciaTotalKm() + mejorDistancia);
            pendientes.remove(mejorParada);
            posicionActual = mejorParada;

        }

        double distanciaVuelta = distanciaService.calcularDistanciaKm(
                posicionActual.getLatitud(), posicionActual.getLongitud(),
                almacen.getLatitud(), almacen.getLongitud()
        );

        double tiempoViajeVueltaMinutos = (distanciaVuelta / VELOCIDAD_MEDIA_KMH) * 60;
        horaActual = horaActual.plusMinutes((long) tiempoViajeVueltaMinutos);

        ruta.agregarParada(almacen);
        ruta.setDistanciaTotalKm(ruta.getDistanciaTotalKm() + distanciaVuelta);

        ruta.setHoraFinEstimada(horaActual);

        Duration tiempoTotal = Duration.between(config.getHoraInicioJornada(), horaActual);
        ruta.setTiempoTotalEstimado(tiempoTotal);

        return ruta;
    }

}
