package com.luispacheco.reparto.service;

import java.time.LocalTime;
import com.luispacheco.reparto.model.Parada;

public class HorarioService {

    public boolean esAlcanzable(LocalTime horaLlegadaEstimada, Parada parada) {
        return !horaLlegadaEstimada.isAfter(parada.getHoraCierre());
    }

    public LocalTime calcularHoraLlegadaConEspera(LocalTime horaLlegadaEstimada, Parada parada) {
        if (horaLlegadaEstimada.isBefore(parada.getHoraApertura())) {
            return parada.getHoraApertura();
        }
        return horaLlegadaEstimada;
    }
}
