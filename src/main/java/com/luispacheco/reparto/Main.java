package com.luispacheco.reparto;

import com.luispacheco.reparto.service.DistanciaService;
import java.time.LocalTime;
import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.model.Ruta;
import com.luispacheco.reparto.model.ConfiguracionReparto;
import com.luispacheco.reparto.service.EnrutadorService;
//import com.luispacheco.reparto.algorithm.HeuristicaVecino;
import com.luispacheco.reparto.service.HorarioService;
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args){
        // Crear el almacen y las paradas
        Parada almacen = new Parada(1, "Almacen Central", "Madrid centro", 40.4168, -3.7038,
                LocalTime.of(8, 0), LocalTime.of(20, 0));

        Parada clienteA = new Parada(2, "Cliente A", "Chamartin", 40.4500, -3.6900,
                LocalTime.of(9, 0), LocalTime.of(13, 0));

        Parada clienteB = new Parada(3, "Cliente B", "Carabanchel", 40.3800, -3.7200,
                LocalTime.of(9, 0), LocalTime.of(18, 0));

        Parada clienteC = new Parada(4, "Cliente C", "Ciudad Lineal", 40.4300, -3.6500,
                LocalTime.of(10, 0), LocalTime.of(14, 0));

        List<Parada> paradas = new ArrayList<>();
        paradas.add(almacen);
        paradas.add(clienteA);
        paradas.add(clienteB);
        paradas.add(clienteC);

// Configuracion del reparto
        ConfiguracionReparto config = new ConfiguracionReparto(almacen, LocalTime.of(8, 0));

// Ejecutar el algoritmo
        EnrutadorService enrutadorService = new EnrutadorService();
        Ruta ruta = enrutadorService.calcularRuta(paradas, config);
        //HeuristicaVecino heuristica = new HeuristicaVecino();
        //Ruta ruta = heuristica.calcular(paradas, config);

// Mostrar resultados
        System.out.println("=== RUTA OPTIMIZADA ===");
        for (Parada p : ruta.getParadasOrdenadas()) {
            System.out.println(p.getNombre() + " - llegada estimada: " + p.getHoraLlegadaEstimada());
        }
        System.out.println("Distancia total: " + ruta.getDistanciaTotalKm() + " km");
        System.out.println("Tiempo total: " + ruta.getTiempoTotalEstimado());
        System.out.println("Hora fin estimada: " + ruta.getHoraFinEstimada());
    }
}
