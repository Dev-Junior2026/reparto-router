package com.luispacheco.reparto.ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.util.List;
import com.luispacheco.reparto.model.Parada;

public class PanelMapa extends BorderPane {

    private WebView webView;
    private WebEngine webEngine;
    private static final String API_KEY = "AIzaSyD63SPYPUbr2ShqRmtXnjboaMwnoVQX8V4";  // REEMPLAZAR CON TU CLAVE

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
        sb.append("    <title>Ruta Calculada</title>\n");
        sb.append("    <script src='https://maps.googleapis.com/maps/api/js?key=").append(API_KEY).append("'></script>\n");
        sb.append("    <style>\n");
        sb.append("        body { margin: 0; padding: 0; font-family: Arial, sans-serif; }\n");
        sb.append("        #map { width: 100%; height: 100%; }\n");
        sb.append("        #info { position: absolute; top: 10px; left: 10px; background: white; padding: 10px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.2); z-index: 1000; }\n");
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("    <div id='info'><strong>Ruta con ").append(paradas.size()).append(" paradas</strong></div>\n");
        sb.append("    <div id='map'></div>\n");
        sb.append("    <script>\n");
        sb.append("        var paradas = [\n");

        // Añadir paradas como marcadores
        for (int i = 0; i < paradas.size(); i++) {
            Parada p = paradas.get(i);
            sb.append("            {lat: ").append(p.getLatitud()).append(", lng: ").append(p.getLongitud())
                    .append(", nombre: '").append(p.getNombre()).append("', orden: ").append(i).append("}\n");
            if (i < paradas.size() - 1) sb.append("            ,");
        }

        sb.append("        ];\n");
        sb.append("        var centerLat = paradas[0].lat;\n");
        sb.append("        var centerLng = paradas[0].lng;\n");
        sb.append("        var map = new google.maps.Map(document.getElementById('map'), {\n");
        sb.append("            zoom: 12,\n");
        sb.append("            center: {lat: centerLat, lng: centerLng}\n");
        sb.append("        });\n");

        // Marcadores
        sb.append("        paradas.forEach(function(p, i) {\n");
        sb.append("            new google.maps.Marker({\n");
        sb.append("                position: {lat: p.lat, lng: p.lng},\n");
        sb.append("                map: map,\n");
        sb.append("                title: p.nombre,\n");
        sb.append("                label: String(i)\n");
        sb.append("            });\n");
        sb.append("        });\n");

        // Línea de ruta
        sb.append("        var path = paradas.map(p => ({lat: p.lat, lng: p.lng}));\n");
        sb.append("        new google.maps.Polyline({\n");
        sb.append("            path: path,\n");
        sb.append("            geodesic: true,\n");
        sb.append("            strokeColor: '#4285F4',\n");
        sb.append("            strokeOpacity: 0.7,\n");
        sb.append("            strokeWeight: 3,\n");
        sb.append("            map: map\n");
        sb.append("        });\n");
        sb.append("    </script>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }
}
