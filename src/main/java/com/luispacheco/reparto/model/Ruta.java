package com.luispacheco.reparto.model;

import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList; // implementación concreta de list

public class Ruta {
    private List<Parada> paradasOrdenadas;
    private double distanciaTotalKm;
    private Duration tiempoTotalEstimado;
    private LocalTime horaInicio;
    private LocalTime horaFinEstimada;

    public Ruta(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
        this.paradasOrdenadas = new ArrayList<>();
        this.distanciaTotalKm = 0.0;
        this.tiempoTotalEstimado = Duration.ZERO; // es una constante predefinida en la clase, que representa una
        // duración de cero (0 segundos, 0 minutos, etc.) equivalente conceptualmente al 0.0 de distanciaTotalKm,
        // pero para el tipo Duration.
        this.horaFinEstimada = null;
    }

    public double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }

    public void setDistanciaTotalKm(double distanciaTotalKm) {
        this.distanciaTotalKm = distanciaTotalKm;
    }

    public LocalTime getHoraFinEstimada() {
        return horaFinEstimada;
    }

    public void setHoraFinEstimada(LocalTime horaFinEstimada) {
        this.horaFinEstimada = horaFinEstimada;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public List<Parada> getParadasOrdenadas() {
        return paradasOrdenadas;
    }

    public void agregarParada(Parada parada) {
        this.paradasOrdenadas.add(parada);
    }

    public Duration getTiempoTotalEstimado() {
        return tiempoTotalEstimado;
    }

    public void setTiempoTotalEstimado(Duration tiempoTotalEstimado) {
        this.tiempoTotalEstimado = tiempoTotalEstimado;
    }
}
