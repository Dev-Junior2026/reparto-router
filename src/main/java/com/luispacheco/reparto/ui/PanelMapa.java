package com.luispacheco.reparto.ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.util.List;
import com.luispacheco.reparto.model.Parada;

public class PanelMapa extends BorderPane {

    private WebView webView;
    private WebEngine webEngine;
    //private static final String API_KEY = "AIzaSyD63SPYPUbr2ShqRmtXnjboaMwnoVQX8V4";  // REEMPLAZAR CON TU CLAVE

    public PanelMapa() {
        webView = new WebView();
        webEngine = webView.getEngine();

        this.setCenter(webView);
        mostrarMapaPorDefecto();
    }

    private void mostrarMapaPorDefecto() {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Mapa de Ruta</title>\n" +
                "    <style>\n" +
                "        body { margin: 0; padding: 0; font-family: Arial, sans-serif; }\n" +
                "        #map { width: 100%; height: 100%; }\n" +
                "        #info { position: absolute; top: 10px; left: 10px; background: white; padding: 10px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.2); }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id='info'>Carga una ruta para ver el mapa</div>\n" +
                "    <div id='map'></div>\n" +
                "</body>\n" +
                "</html>";
        webEngine.loadContent(html);
    }

    public void mostrarRuta(List<Parada> paradas) {
        if (paradas == null || paradas.isEmpty()) {
            mostrarMapaPorDefecto();
            return;
        }

        String html = construirMapaHTML(paradas);
        webEngine.loadContent(html);
    }

    private String construirMapaHTML(List<Parada> paradas) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("    <meta charset='UTF-8'>\n");
        sb.append("    <title>Ruta</title>\n");
        sb.append("    <style>\n");
        sb.append("        body { font-family: Arial; margin: 20px; background: #f5f5f5; }\n");
        sb.append("        h1 { color: #333; }\n");
        sb.append("        table { border-collapse: collapse; width: 100%; background: white; }\n");
        sb.append("        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }\n");
        sb.append("        th { background-color: #4285F4; color: white; }\n");
        sb.append("        tr:hover { background-color: #f9f9f9; }\n");
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("    <h1>Ruta Calculada - ").append(paradas.size()).append(" Paradas</h1>\n");
        sb.append("    <table>\n");
        sb.append("        <tr><th>Orden</th><th>Nombre</th><th>Latitud</th><th>Longitud</th></tr>\n");

        for (int i = 0; i < paradas.size(); i++) {
            Parada p = paradas.get(i);
            sb.append("        <tr>\n");
            sb.append("            <td>").append(i).append("</td>\n");
            sb.append("            <td>").append(p.getNombre()).append("</td>\n");
            sb.append("            <td>").append(String.format("%.4f", p.getLatitud())).append("</td>\n");
            sb.append("            <td>").append(String.format("%.4f", p.getLongitud())).append("</td>\n");
            sb.append("        </tr>\n");
        }

        sb.append("    </table>\n");
        sb.append("    <p style='margin-top: 20px; color: #666;'><em>Nota: Para un mapa interactivo, se recomienda usar OpenStreetMap desde navegador externo.</em></p>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }
}
