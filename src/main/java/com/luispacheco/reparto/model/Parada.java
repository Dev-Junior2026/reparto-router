package com.luispacheco.reparto.model;

import java.time.LocalTime;

public class Parada {
    private int numero;
    private String nombre;
    private String calle;
    private String codigoPostal;
    private String poblacion;
    private double latitud;
    private double longitud;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private int tiempoDescargaMin;
    private boolean esAlmacen;
    private LocalTime horaLlegadaEstimada;

    /**
     * Constructor principal. lat/lon se inicializan a 0.0 y se asignan
     * después mediante setLatitud/setLongitud tras la geocodificación.
     */

    public Parada(int numero, String nombre, String calle, String codigoPostal,
                  String poblacion, LocalTime horaApertura, LocalTime horaCierre) {
        this.numero = numero;
        this.nombre = nombre;
        this.calle = calle;
        this.codigoPostal = codigoPostal;
        this.poblacion = poblacion;
        this.latitud = 0.0;
        this.longitud = 0.0;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.tiempoDescargaMin = 15;
        this.esAlmacen = (numero == 1);
        this.horaLlegadaEstimada = null;
    }


    public Parada(int numero, String nombre, String calle, String codigoPostal,
                  String poblacion, double lat, double lon,
                  LocalTime abre, LocalTime cierra) {
        this.numero = numero;
        this.nombre = nombre;
        this.calle = calle;
        this.codigoPostal = codigoPostal;
        this.poblacion = poblacion;
        this.latitud = lat;
        this.longitud = lon;
        this.horaApertura = abre;
        this.horaCierre = cierra;
        this.tiempoDescargaMin = 15;
        this.esAlmacen = (numero == 1);
        this.horaLlegadaEstimada = null;
    }

    /**
     * Devuelve la dirección completa formateada:
     * "Calle Calidad, 68 28906 Getafe"
     */
    public String getDireccion() {
        return calle + " " + codigoPostal + " " + poblacion;
    }

    // ── Getters y Setters ─────────────────────────────────────────────────────

    public int getNumero() { return numero; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getPoblacion() { return poblacion; }
    public void setPoblacion(String poblacion) { this.poblacion = poblacion; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public int getTiempoDescargaMin() { return tiempoDescargaMin; }
    public void setTiempoDescargaMin(int tiempoDescargaMin) { this.tiempoDescargaMin = tiempoDescargaMin; }

    public boolean isEsAlmacen() { return esAlmacen; }

    public LocalTime getHoraLlegadaEstimada() { return horaLlegadaEstimada; }
    public void setHoraLlegadaEstimada(LocalTime horaLlegadaEstimada) {
        this.horaLlegadaEstimada = horaLlegadaEstimada;
    }
}
