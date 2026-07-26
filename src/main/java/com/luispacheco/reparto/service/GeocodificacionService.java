package com.luispacheco.reparto.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GeocodificacionService {

    private final HttpClient httpClient;

    public GeocodificacionService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Llama a Nominatim (OpenStreetMap) para obtener lat/lon de una dirección.
     * @return double[] de dos elementos: [latitud, longitud], o null si falla.
     */
    public double[] geocodificar(String direccionCompleta) {
        try {
            String query = URLEncoder.encode(direccionCompleta, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + query
                    + "&format=json&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "RepartoRouter/1.0")
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body();
            if (body == null || body.isEmpty() || body.equals("[]")) {
                return null;
            }

            // Parse manual del JSON sin librería externa
            String lat = extraerValor(body, "\"lat\":\"", "\"");
            String lon = extraerValor(body, "\"lon\":\"", "\"");

            if (lat == null || lon == null) return null;

            return new double[]{ Double.parseDouble(lat), Double.parseDouble(lon) };

        } catch (Exception e) {
            return null;
        }
    }

    private String extraerValor(String json, String clave, String fin) {
        int start = json.indexOf(clave);
        if (start == -1) return null;
        start += clave.length();
        int end = json.indexOf(fin, start);
        return (end == -1) ? null : json.substring(start, end);
    }
}