package com.luispacheco.reparto.service;

import com.luispacheco.reparto.algorithm.HeuristicaVecino;
import com.luispacheco.reparto.model.ConfiguracionReparto;
import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.model.Ruta;

import java.util.List;

public class EnrutadorService {

    private HeuristicaVecino heuristicaVecino;

    public EnrutadorService() {
        this.heuristicaVecino = new HeuristicaVecino();
    }

    public Ruta calcularRuta(List<Parada> paradas, ConfiguracionReparto config) {
        return heuristicaVecino.calcular(paradas, config);
    }
}