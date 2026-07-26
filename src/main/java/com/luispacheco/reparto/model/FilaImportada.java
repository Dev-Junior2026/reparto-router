package com.luispacheco.reparto.model;

import java.time.LocalTime;

/**
 * Representa una fila extraída del PDF de reparto, antes de geocodificar.
 * Se usa en la pantalla de previsualización del importador.
 */
public class FilaImportada {
    private String nombre;
    private String calle;
    private String codigoPostal;
    private String poblacion;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private boolean horarioDetectado; // false si no se pudo leer un rango válido

    public FilaImportada(String nombre, String calle, String codigoPostal, String poblacion,
                         LocalTime horaApertura, LocalTime horaCierre, boolean horarioDetectado) {
        this.nombre = nombre;
        this.calle = calle;
        this.codigoPostal = codigoPostal;
        this.poblacion = poblacion;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.horarioDetectado = horarioDetectado;
    }

    public String getNombre() { return nombre; }
    public String getCalle() { return calle; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getPoblacion() { return poblacion; }
    public LocalTime getHoraApertura() { return horaApertura; }
    public LocalTime getHoraCierre() { return horaCierre; }
    public boolean isHorarioDetectado() { return horarioDetectado; }

    // Setters añadidos para permitir editar la fila en la ventana de
    // previsualización antes de geocodificarla (p.ej. corregir direcciones
    // que Nominatim no reconoce: "LOCAL 10B", tipos de vía duplicados, etc.)
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCalle(String calle) { this.calle = calle; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public void setPoblacion(String poblacion) { this.poblacion = poblacion; }
}