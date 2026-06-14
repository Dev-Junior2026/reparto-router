package com.luispacheco.reparto;

import com.luispacheco.reparto.service.distanciaService;

public class Main {
    public static void main(String[] args){
        System.out.println("Reparto Router - fase 1 iniciada");

        distanciaService distanciaService = new distanciaService();
        double distancia = distanciaService.calcularDistanciaKm(40.4168, -3.7038, 41.3851, 2.1734);
        System.out.println("Distancia Madrid-Barcelona: " + distancia + " km");
    }
}
