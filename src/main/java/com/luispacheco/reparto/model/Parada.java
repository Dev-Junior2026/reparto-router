package com.luispacheco.reparto.model;

import java.time.LocalTime;

public class Parada {
    private int numero; // 1 = almacen, 2..N = entregas
    private String nombre;
    private String direccion;
    private double latitud;
    private double longitud;
    private LocalTime horaApertura; // hora minima de entrega
    private LocalTime horaCierre; // hora limite de entrega
    private int tiempoDescargaMin; // por defecto 15 minutos
    private boolean esAlmacen; // true solo para parada 1
    private LocalTime horaLlegadaEstimada; // se calcula al enrutar

    public Parada(int numero, String nombre, String direccion, double latitud, double longitud,
                  LocalTime horaApertura, LocalTime horaCierre) {
        // El constructor tiene el mismo nombre que la clase
        // This se refiere a un objeto en concreto
        this.numero = numero;
        this.nombre = nombre;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.tiempoDescargaMin = 15;           // valor por defecto
        this.esAlmacen = (numero == 1);        // se calcula solo
        this.horaLlegadaEstimada = null;       // aun no se conoce
    }

    public int getNumero() {
        return numero;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime horaApertura) {
        this.horaApertura = horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(LocalTime horaCierre) {
        this.horaCierre = horaCierre;
    }

    public int getTiempoDescargaMin() {
        return tiempoDescargaMin;
    }

    public void setTiempoDescargaMin(int tiempoDescargaMin) {
        this.tiempoDescargaMin = tiempoDescargaMin;
    }

    public boolean isEsAlmacen() {
        return esAlmacen;
    }

    public LocalTime getHoraLlegadaEstimada() {
        return horaLlegadaEstimada;
    }

    public void setHoraLlegadaEstimada(LocalTime horaLlegadaEstimada) {
        this.horaLlegadaEstimada = horaLlegadaEstimada;
    }
}
