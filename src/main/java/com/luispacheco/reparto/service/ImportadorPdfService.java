package com.luispacheco.reparto.service;

import com.luispacheco.reparto.model.FilaImportada;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportadorPdfService {

    // Índices de columna según el formato del informe (0 = primera columna, "Orden")
    private static final int COL_ALIAS = 2;
    private static final int COL_DIRECCION = 3;
    private static final int COL_CP = 4;
    private static final int COL_POBLACION = 5;
    private static final int COL_HORA = 7;

    private static final Pattern PATRON_RANGO_HORARIO =
            Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");

    public List<FilaImportada> extraerFilas(File archivoPdf) throws IOException {
        List<FilaImportada> resultado = new ArrayList<>();

        try (PDDocument documento = PDDocument.load(archivoPdf)) {
            ObjectExtractor extractor = new ObjectExtractor(documento);
            SpreadsheetExtractionAlgorithm algoritmo = new SpreadsheetExtractionAlgorithm();

            for (int numPagina = 1; numPagina <= documento.getNumberOfPages(); numPagina++) {
                Page pagina = extractor.extract(numPagina);
                List<Table> tablas = algoritmo.extract(pagina);

                for (Table tabla : tablas) {
                    List<List<RectangularTextContainer>> filas = tabla.getRows();

                    for (int i = 0; i < filas.size(); i++) {
                        List<RectangularTextContainer> celdas = filas.get(i);

                        if (celdas.size() <= COL_HORA) continue; // fila con menos columnas de lo esperado, se ignora
                        if (esFilaDeCabecera(celdas)) continue;

                        String alias = limpiar(celdas.get(COL_ALIAS).getText());
                        String direccion = limpiar(celdas.get(COL_DIRECCION).getText());
                        String cp = limpiar(celdas.get(COL_CP).getText());
                        String poblacion = limpiar(celdas.get(COL_POBLACION).getText());
                        String horaTexto = limpiar(celdas.get(COL_HORA).getText());

                        if (alias.isEmpty() || direccion.isEmpty() || cp.isEmpty()) continue;

                        LocalTime[] horario = parsearHorario(horaTexto);
                        boolean detectado = horario != null;
                        LocalTime apertura = detectado ? horario[0] : LocalTime.of(9, 0);
                        LocalTime cierre = detectado ? horario[1] : LocalTime.of(20, 0);

                        resultado.add(new FilaImportada(alias, direccion, cp, poblacion,
                                apertura, cierre, detectado));
                    }
                }
            }
        }

        return resultado;
    }

    private boolean esFilaDeCabecera(List<RectangularTextContainer> celdas) {
        String alias = limpiar(celdas.get(COL_ALIAS).getText());
        return alias.equalsIgnoreCase("Alias");
    }

    private LocalTime[] parsearHorario(String texto) {
        Matcher m = PATRON_RANGO_HORARIO.matcher(texto);
        if (!m.find()) return null;
        try {
            LocalTime apertura = LocalTime.parse(normalizarHora(m.group(1)));
            LocalTime cierre = LocalTime.parse(normalizarHora(m.group(2)));
            if (cierre.isBefore(apertura) || cierre.equals(apertura)) return null;
            return new LocalTime[]{apertura, cierre};
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizarHora(String hora) {
        // LocalTime.parse exige HH:mm con dos dígitos; "9:00" -> "09:00"
        String[] partes = hora.split(":");
        return String.format("%02d:%s", Integer.parseInt(partes[0]), partes[1]);
    }

    private String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }
}