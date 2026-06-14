package com.luispacheco.reparto;

import com.luispacheco.reparto.service.DistanciaService;
import java.time.LocalTime;
import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.service.HorarioService;

public class Main {
    public static void main(String[] args){
        System.out.println("Reparto Router - fase 1 iniciada");

        DistanciaService distanciaService = new DistanciaService();
        double distancia = distanciaService.calcularDistanciaKm(40.4168, -3.7038, 41.3851,
                2.1734);
        System.out.println("Distancia Madrid-Barcelona: " + distancia + " km");

        Parada cliente1 = new Parada(2, "Cliente 1", "Calle Falsa 123", 40.42,
                -3.70, LocalTime.of(10, 0), LocalTime.of(17, 0));

        HorarioService horarioService = new HorarioService();
        LocalTime horaLlegada = LocalTime.of(9, 0);

        boolean alcanzable = horarioService.esAlcanzable(horaLlegada, cliente1);
        LocalTime horaLlegadaReal = horarioService.calcularHoraLlegadaConEspera(horaLlegada, cliente1);

        System.out.println("Alcanzable: " + alcanzable);
        System.out.println("Hora real de llegada (con espera): " + horaLlegadaReal);
    }
}
