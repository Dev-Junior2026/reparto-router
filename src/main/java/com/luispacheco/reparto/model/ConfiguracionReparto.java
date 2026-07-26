package com.luispacheco.reparto.model;

import java.time.LocalTime;

public class ConfiguracionReparto {
    private Parada lugarCargaDescarga; // Parada numero 1
    private LocalTime horaInicioJornada;
    private int tiempoDescargaDefecto; // 15 min por defecto
    private String navegadorPreferido; // "google", "waze", etc.

    public ConfiguracionReparto(Parada lugarCargaDescarga, LocalTime horaInicioJornada) {
        this.lugarCargaDescarga = lugarCargaDescarga;
        this.horaInicioJornada = horaInicioJornada;
        this.tiempoDescargaDefecto = 15;        // valor por defecto
        this.navegadorPreferido = "google";     // valor por defecto
    }

    public Parada getLugarCargaDescarga() {
        return lugarCargaDescarga;
    }

    public void setLugarCargaDescarga(Parada lugarCargaDescarga) {
        this.lugarCargaDescarga = lugarCargaDescarga;
    }

    public LocalTime getHoraInicioJornada() {
        return horaInicioJornada;
    }

    public void setHoraInicioJornada(LocalTime horaInicioJornada) {
        this.horaInicioJornada = horaInicioJornada;
    }

    public int getTiempoDescargaDefecto() {
        return tiempoDescargaDefecto;
    }

    public void setTiempoDescargaDefecto(int tiempoDescargaDefecto) {
        this.tiempoDescargaDefecto = tiempoDescargaDefecto;
    }

    public String getNavegadorPreferido() {
        return navegadorPreferido;
    }

    public void setNavegadorPreferido(String navegadorPreferido) {
        this.navegadorPreferido = navegadorPreferido;
    }
}
