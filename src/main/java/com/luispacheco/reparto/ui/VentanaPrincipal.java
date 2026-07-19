package com.luispacheco.reparto.ui;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.model.Ruta;
import com.luispacheco.reparto.model.ConfiguracionReparto;
import com.luispacheco.reparto.service.EnrutadorService;
import com.luispacheco.reparto.service.GeocodificacionService;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;

public class VentanaPrincipal extends Application {

    private EnrutadorService enrutadorService;
    private GeocodificacionService geocodificacionService;

    // Panel izquierdo: entrada de paradas
    private TextField txtNombre;
    private TextField txtCalle;
    private TextField txtCodigoPostal;
    private TextField txtPoblacion;
    private Spinner<Integer> spinHoraApertura;
    private Spinner<Integer> spinMinutoApertura;
    private Spinner<Integer> spinHoraCierre;
    private Spinner<Integer> spinMinutoCierre;
    private TableView<Parada> tablaParadasIngresadas;
    private ObservableList<Parada> paradasIngresadas;
    private Parada almacenGlobal;
    private Button btnCalcularRuta;

    // Almacén
    private TextField txtNombreAlmacen;
    private TextField txtCalleAlmacen;
    private TextField txtCodigoPostalAlmacen;
    private TextField txtPoblacionAlmacen;
    private Spinner<Integer> spinHoraInicioRuta;
    private Spinner<Integer> spinMinutoInicioRuta;

    // Panel derecho: resultados
    private TableView<Parada> tablaParadas;
    private Label lblDistancia;
    private Label lblTiempo;
    private Label lblHoraFin;
    private Button btnVerEnMapa;

    // Navegación paso a paso
    private int paradaActualIndex = -1;
    private Button btnEmpezarRuta;
    private Button btnMarcarCompletada;
    private Label lblParadaActual;

    // Última ruta calculada (para el botón Ver en mapa)
    private List<Parada> ultimaRuta;

    private HttpServer servidorMapa;
    private int puertoMapa;

    @Override
    public void start(Stage primaryStage) {
        enrutadorService = new EnrutadorService();
        geocodificacionService = new GeocodificacionService();
        paradasIngresadas = FXCollections.observableArrayList();
        almacenGlobal = null;
        ultimaRuta = new ArrayList<>();
        paradaActualIndex = -1;
        iniciarServidorMapa();

        VBox panelIzquierdo = crearPanelEntrada();
        VBox panelDerecho = crearPanelResultados();

        SplitPane splitPane = new SplitPane(panelIzquierdo, panelDerecho);
        splitPane.setDividerPositions(0.45);

        Scene escena = new Scene(splitPane, 1500, 750);
        primaryStage.setTitle("Reparto Router");
        primaryStage.setScene(escena);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (servidorMapa != null) {
            servidorMapa.stop(0);
        }
    }

    private void iniciarServidorMapa() {
        try {
            servidorMapa = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidorMapa.start();
            puertoMapa = servidorMapa.getAddress().getPort();
            registrarContextoMapa();
        } catch (Exception ex) {
            mostrarAlerta("Error", "No se pudo iniciar el servidor del mapa: " + ex.getMessage());
        }
    }

    private void registrarContextoMapa() {
        servidorMapa.createContext("/mapa", exchange -> {
            String html = generarHtmlLeaflet(paradaActualIndex);
            byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private VBox crearPanelEntrada() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label titulo = new Label("Gesti\u00f3n de Paradas");
        titulo.setStyle("-fx-font-size: 14; -fx-font-weight: bold");

        // === FORMULARIO PARA ALMACÉN ===
        Label lblAlmacen = new Label("Almac\u00e9n (Parada 1)");
        lblAlmacen.setStyle("-fx-font-weight: bold");

        txtNombreAlmacen = new TextField();
        txtNombreAlmacen.setPromptText("Ej: Almac\u00e9n Central");
        txtNombreAlmacen.setPrefWidth(140);

        txtCalleAlmacen = new TextField();
        txtCalleAlmacen.setPromptText("Calle y n\u00famero");
        txtCalleAlmacen.setPrefWidth(140);

        txtCodigoPostalAlmacen = new TextField();
        txtCodigoPostalAlmacen.setPromptText("C.P. ej: 28906");
        txtCodigoPostalAlmacen.setPrefWidth(80);

        txtPoblacionAlmacen = new TextField();
        txtPoblacionAlmacen.setPromptText("Poblaci\u00f3n");
        txtPoblacionAlmacen.setPrefWidth(120);

        spinHoraInicioRuta = new Spinner<>(0, 23, 8);
        spinHoraInicioRuta.setPrefWidth(55);
        spinMinutoInicioRuta = new Spinner<>(0, 59, 0);
        spinMinutoInicioRuta.setPrefWidth(55);

        Button btnCrearAlmacen = new Button("Crear Almac\u00e9n");
        btnCrearAlmacen.setOnAction(e -> crearAlmacen());

        HBox almacenBox = new HBox(6);
        almacenBox.getChildren().addAll(
                new Label("Nombre:"), txtNombreAlmacen,
                new Label("Calle:"), txtCalleAlmacen,
                new Label("CP:"), txtCodigoPostalAlmacen,
                new Label("Pob:"), txtPoblacionAlmacen,
                new Label("Inicio:"), spinHoraInicioRuta, new Label(":"), spinMinutoInicioRuta,
                btnCrearAlmacen
        );
        almacenBox.setStyle("-fx-border-bottom: 2px solid #ccc; -fx-padding: 10");

        // === FORMULARIO PARA CLIENTES ===
        Label lblClientes = new Label("A\u00f1adir Clientes");
        lblClientes.setStyle("-fx-font-weight: bold");

        txtNombre = new TextField();
        txtNombre.setPromptText("Nombre cliente");
        txtNombre.setPrefWidth(130);

        txtCalle = new TextField();
        txtCalle.setPromptText("Calle y n\u00famero");
        txtCalle.setPrefWidth(130);

        txtCodigoPostal = new TextField();
        txtCodigoPostal.setPromptText("C.P. ej: 28906");
        txtCodigoPostal.setPrefWidth(70);

        txtPoblacion = new TextField();
        txtPoblacion.setPromptText("Poblaci\u00f3n");
        txtPoblacion.setPrefWidth(110);

        spinHoraApertura = new Spinner<>(0, 23, 9);
        spinHoraApertura.setPrefWidth(50);
        spinMinutoApertura = new Spinner<>(0, 59, 0);
        spinMinutoApertura.setPrefWidth(50);
        spinHoraCierre = new Spinner<>(0, 23, 13);
        spinHoraCierre.setPrefWidth(50);
        spinMinutoCierre = new Spinner<>(0, 59, 0);
        spinMinutoCierre.setPrefWidth(50);

        Button btnAnadirParada = new Button("A\u00f1adir Parada");
        btnAnadirParada.setOnAction(e -> anadirParada());

        HBox formularioBox = new HBox(5);
        formularioBox.getChildren().addAll(
                new Label("Nombre:"), txtNombre,
                new Label("Calle:"), txtCalle,
                new Label("CP:"), txtCodigoPostal,
                new Label("Pob:"), txtPoblacion,
                new Label("Abre:"), spinHoraApertura, new Label(":"), spinMinutoApertura,
                new Label("Cierra:"), spinHoraCierre, new Label(":"), spinMinutoCierre,
                btnAnadirParada
        );

        // === TABLA DE PARADAS INGRESADAS ===
        Label lblParadasIngresadas = new Label("Paradas ingresadas:");
        lblParadasIngresadas.setStyle("-fx-font-weight: bold");

        tablaParadasIngresadas = new TableView<>();
        tablaParadasIngresadas.setItems(paradasIngresadas);

        TableColumn<Parada, String> col1 = new TableColumn<>("Nombre");
        col1.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        col1.setPrefWidth(130);

        TableColumn<Parada, String> col2 = new TableColumn<>("Direcci\u00f3n");
        col2.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        col2.setPrefWidth(200);

        TableColumn<Parada, String> colHorario = new TableColumn<>("Horario");
        colHorario.setCellValueFactory(cellData -> {
            Parada p = cellData.getValue();
            String texto = (p.getHoraApertura() != null && p.getHoraCierre() != null)
                    ? p.getHoraApertura() + " - " + p.getHoraCierre()
                    : "-";
            return new javafx.beans.property.SimpleStringProperty(texto);
        });
        colHorario.setPrefWidth(110);

        TableColumn<Parada, Void> colEliminar = new TableColumn<>("Acci\u00f3n");
        colEliminar.setCellFactory(column -> new TableCell<Parada, Void>() {
            private final Button btn = new Button("Eliminar");
            {
                btn.setStyle("-fx-font-size: 10");
                btn.setOnAction(event -> {
                    Parada parada = getTableView().getItems().get(getIndex());
                    paradasIngresadas.remove(parada);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        colEliminar.setPrefWidth(70);

        tablaParadasIngresadas.getColumns().addAll(col1, col2, colHorario, colEliminar);
        tablaParadasIngresadas.setPrefHeight(180);

        // === BOTÓN CALCULAR RUTA ===
        btnCalcularRuta = new Button("Calcular Ruta");
        btnCalcularRuta.setPrefWidth(150);
        btnCalcularRuta.setStyle("-fx-font-size: 12; -fx-padding: 8");
        btnCalcularRuta.setDisable(true);
        btnCalcularRuta.setOnAction(e -> calcularRuta());

        panel.getChildren().addAll(
                titulo,
                new Separator(),
                lblAlmacen, almacenBox,
                new Separator(),
                lblClientes, formularioBox,
                new Separator(),
                lblParadasIngresadas, tablaParadasIngresadas,
                btnCalcularRuta
        );

        return panel;
    }

    private VBox crearPanelResultados() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label titulo = new Label("Ruta Calculada");
        titulo.setStyle("-fx-font-size: 14; -fx-font-weight: bold");

        tablaParadas = new TableView<>();

        // Colorea las filas según progreso de la ruta
        tablaParadas.setRowFactory(tv -> new TableRow<Parada>() {
            @Override
            protected void updateItem(Parada item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                int idx = getIndex();
                if      (paradaActualIndex < 0)       setStyle("");
                else if (idx < paradaActualIndex)      setStyle("-fx-background-color: #bdc3c7;"); // gris - completada
                else if (idx == paradaActualIndex)     setStyle("-fx-background-color: #2ecc71;"); // verde - actual
                else                                   setStyle(""); // pendiente
            }
        });

        TableColumn<Parada, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(130);

        TableColumn<Parada, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colDireccion.setPrefWidth(200);

        TableColumn<Parada, String> colHorarioRuta = new TableColumn<>("Horario");
        colHorarioRuta.setCellValueFactory(cellData -> {
            Parada p = cellData.getValue();
            String texto = (p.getHoraApertura() != null && p.getHoraCierre() != null)
                    ? p.getHoraApertura() + " - " + p.getHoraCierre()
                    : "-";
            return new javafx.beans.property.SimpleStringProperty(texto);
        });
        colHorarioRuta.setPrefWidth(110);

        TableColumn<Parada, LocalTime> colHoraLlegada = new TableColumn<>("Hora Llegada");
        colHoraLlegada.setCellValueFactory(new PropertyValueFactory<>("horaLlegadaEstimada"));
        colHoraLlegada.setCellFactory(column -> new TableCell<Parada, LocalTime>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "-" : item.toString());
            }
        });
        colHoraLlegada.setPrefWidth(110);

        tablaParadas.getColumns().addAll(colNombre, colDireccion, colHorarioRuta, colHoraLlegada);
        VBox.setVgrow(tablaParadas, Priority.ALWAYS);

        // Botones principales
        btnVerEnMapa = new Button("Ver en mapa");
        btnVerEnMapa.setPrefWidth(150);
        btnVerEnMapa.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        btnVerEnMapa.setDisable(true);
        btnVerEnMapa.setOnAction(e -> abrirEnMapa());

        btnEmpezarRuta = new Button("Empezar Ruta");
        btnEmpezarRuta.setPrefWidth(150);
        btnEmpezarRuta.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #2196F3; -fx-text-fill: white;");
        btnEmpezarRuta.setDisable(true);
        btnEmpezarRuta.setOnAction(e -> empezarRuta());

        HBox botonesMapaRuta = new HBox(10, btnVerEnMapa, btnEmpezarRuta);

        // Navegación paso a paso
        lblParadaActual = new Label("");
        lblParadaActual.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

        btnMarcarCompletada = new Button("✓  Marcar como completada");
        btnMarcarCompletada.setPrefWidth(220);
        btnMarcarCompletada.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #FF9800; -fx-text-fill: white;");
        btnMarcarCompletada.setDisable(true);
        btnMarcarCompletada.setOnAction(e -> marcarParadaCompletada());

        // Panel info
        lblDistancia = new Label("Distancia total: -");
        lblTiempo    = new Label("Tiempo total: -");
        lblHoraFin   = new Label("Hora fin: -");

        HBox panelInfo = new HBox(20);
        panelInfo.setPadding(new Insets(10));
        panelInfo.setStyle("-fx-border-top: 2px solid #ccc");
        panelInfo.getChildren().addAll(lblDistancia, lblTiempo, lblHoraFin);

        panel.getChildren().addAll(
                titulo, tablaParadas,
                botonesMapaRuta,
                lblParadaActual, btnMarcarCompletada,
                panelInfo
        );
        return panel;
    }

    private void crearAlmacen() {
        String nombre = txtNombreAlmacen.getText().trim();
        String calle = txtCalleAlmacen.getText().trim();
        String cp = txtCodigoPostalAlmacen.getText().trim();
        String poblacion = txtPoblacionAlmacen.getText().trim();

        if (nombre.isEmpty() || calle.isEmpty() || cp.isEmpty() || poblacion.isEmpty()) {
            mostrarAlerta("Error", "Completa todos los campos del almac\u00e9n (nombre, calle, CP, poblaci\u00f3n)");
            return;
        }

        String direccionCompleta = calle + ", " + cp + " " + poblacion;
        double[] coords = geocodificacionService.geocodificar(direccionCompleta);

        if (coords == null) {
            mostrarAlerta("Error", "No se pudo geocodificar la direcci\u00f3n. Verifica que sea correcta.");
            return;
        }

        almacenGlobal = new Parada(1, nombre, calle, cp, poblacion,
                LocalTime.of(8, 0), LocalTime.of(20, 0));
        almacenGlobal.setLatitud(coords[0]);
        almacenGlobal.setLongitud(coords[1]);

        btnCalcularRuta.setDisable(false);
        mostrarAlerta("\u00c9xito", "Almac\u00e9n creado: " + nombre);
    }

    private void anadirParada() {
        if (almacenGlobal == null) {
            mostrarAlerta("Error", "Crea el almac\u00e9n primero");
            return;
        }

        String nombre = txtNombre.getText().trim();
        String calle = txtCalle.getText().trim();
        String cp = txtCodigoPostal.getText().trim();
        String poblacion = txtPoblacion.getText().trim();

        if (nombre.isEmpty() || calle.isEmpty() || cp.isEmpty() || poblacion.isEmpty()) {
            mostrarAlerta("Error", "Completa nombre, calle, CP y poblaci\u00f3n de la parada");
            return;
        }

        String direccionCompleta = calle + ", " + cp + " " + poblacion;
        double[] coords = geocodificacionService.geocodificar(direccionCompleta);

        if (coords == null) {
            mostrarAlerta("Error", "No se pudo geocodificar la direcci\u00f3n. Verifica que sea correcta.");
            return;
        }

        int horaApertura = spinHoraApertura.getValue();
        int minutoApertura = spinMinutoApertura.getValue();
        int horaCierre = spinHoraCierre.getValue();
        int minutoCierre = spinMinutoCierre.getValue();

        LocalTime abre = LocalTime.of(horaApertura, minutoApertura);
        LocalTime cierra = LocalTime.of(horaCierre, minutoCierre);
        if (cierra.isBefore(abre) || cierra.equals(abre)) {
            mostrarAlerta("Error", "La hora de cierre debe ser despu\u00e9s de apertura");
            return;
        }

        Parada nueva = new Parada(
                paradasIngresadas.size() + 2,
                nombre, calle, cp, poblacion,
                abre, cierra
        );
        nueva.setLatitud(coords[0]);
        nueva.setLongitud(coords[1]);

        paradasIngresadas.add(nueva);

        txtNombre.clear();
        txtCalle.clear();
        txtCodigoPostal.clear();
        txtPoblacion.clear();
        spinHoraApertura.getValueFactory().setValue(9);
        spinMinutoApertura.getValueFactory().setValue(0);
        spinHoraCierre.getValueFactory().setValue(13);
        spinMinutoCierre.getValueFactory().setValue(0);

        mostrarAlerta("\u00c9xito", "Parada a\u00f1adida: " + nombre);
    }

    private void calcularRuta() {
        if (almacenGlobal == null) {
            mostrarAlerta("Error", "Crea el almac\u00e9n primero");
            return;
        }

        if (paradasIngresadas.isEmpty()) {
            mostrarAlerta("Error", "A\u00f1ade al menos una parada");
            return;
        }

        List<Parada> todasLasParadas = new ArrayList<>();
        todasLasParadas.add(almacenGlobal);
        todasLasParadas.addAll(paradasIngresadas);

        LocalTime horaInicio = LocalTime.of(spinHoraInicioRuta.getValue(), spinMinutoInicioRuta.getValue());
        ConfiguracionReparto config = new ConfiguracionReparto(almacenGlobal, horaInicio);

        Ruta ruta = enrutadorService.calcularRuta(todasLasParadas, config);

        tablaParadas.getItems().clear();
        tablaParadas.getItems().addAll(ruta.getParadasOrdenadas());

        ultimaRuta = new ArrayList<>(ruta.getParadasOrdenadas());

        lblDistancia.setText(String.format("Distancia total: %.2f km", ruta.getDistanciaTotalKm()));
        lblTiempo.setText("Tiempo total: " + ruta.getTiempoTotalEstimado());
        lblHoraFin.setText("Hora fin: " + ruta.getHoraFinEstimada());

        btnVerEnMapa.setDisable(false);

        // Resetear navegación al recalcular
        paradaActualIndex = -1;
        lblParadaActual.setText("");
        lblParadaActual.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        btnEmpezarRuta.setDisable(false);
        btnMarcarCompletada.setDisable(true);
        tablaParadas.refresh();
    }

    private void abrirEnMapa() {
        if (ultimaRuta == null || ultimaRuta.isEmpty()) {
            mostrarAlerta("Error", "No hay ruta calculada");
            return;
        }
        abrirMapaLeaflet(paradaActualIndex); // pasa el estado actual (-1 si aún no empezó)
    }

    private void abrirMapaLeaflet(int paradaActiva) {
        try {
            String url = "http://localhost:" + puertoMapa + "/mapa";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                mostrarAlerta("Info", "Abre esta URL en tu navegador:\n" + url);
            }
        } catch (Exception ex) {
            mostrarAlerta("Error", "No se pudo abrir el mapa: " + ex.getMessage());
        }
    }

    private String generarHtmlLeaflet(int paradaActiva) {
        StringBuilder sb = new StringBuilder();
        double cLat = ultimaRuta.stream().mapToDouble(Parada::getLatitud).average().orElse(40.416);
        double cLon = ultimaRuta.stream().mapToDouble(Parada::getLongitud).average().orElse(-3.703);

        sb.append("<!DOCTYPE html><html><head>")
                .append("<meta charset='utf-8'/>")
                .append("<title>Reparto Router - Mapa</title>")
                .append("<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>")
                .append("<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>")
                .append("<style>")
                .append("body{margin:0;padding:0;}")
                .append("#map{width:100%;height:100vh;}")
                .append(".nm{display:flex;align-items:center;justify-content:center;")
                .append("width:30px;height:30px;border-radius:50%;color:white;")
                .append("font-weight:bold;font-size:13px;border:2px solid white;")
                .append("box-shadow:0 2px 5px rgba(0,0,0,.4);}")
                .append("</style></head><body><div id='map'></div><script>")
                .append("var map=L.map('map').setView([").append(cLat).append(",").append(cLon).append("],13);")
                .append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{")
                .append("maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(map);");

        sb.append("function irHacia(lat,lon){")
                .append("if(!navigator.geolocation){alert('Tu navegador no soporta geolocalizaci\u00f3n');return;}")
                .append("navigator.geolocation.getCurrentPosition(function(pos){")
                .append("var url='https://www.google.com/maps/dir/?api=1&origin='+pos.coords.latitude+','+pos.coords.longitude+'&destination='+lat+','+lon+'&travelmode=driving';")
                .append("window.open(url,'_blank');")
                .append("},function(err){alert('No se pudo obtener tu ubicaci\u00f3n: '+err.message);});")
                .append("}");

        // Polilínea cerrando el ciclo al almacén
        sb.append("L.polyline([");
        for (int i = 0; i < ultimaRuta.size(); i++) {
            Parada p = ultimaRuta.get(i);
            if (i > 0) sb.append(",");
            sb.append("[").append(p.getLatitud()).append(",").append(p.getLongitud()).append("]");
        }
        Parada a = ultimaRuta.get(0);
        sb.append(",[").append(a.getLatitud()).append(",").append(a.getLongitud()).append("]")
                .append("],{color:'#2980b9',weight:3,opacity:.7}).addTo(map);");

        // Marcadores numerados con colores por estado
        for (int i = 0; i < ultimaRuta.size(); i++) {
            Parada p = ultimaRuta.get(i);

            String color;
            if      (paradaActiva < 0)           color = (i == 0) ? "#e74c3c" : "#2980b9";
            else if (i < paradaActiva)           color = "#7f8c8d"; // completada - gris
            else if (i == paradaActiva)          color = "#27ae60"; // actual - verde
            else                                 color = "#2980b9"; // pendiente - azul

            String nombre = escHtml(p.getNombre());
            String dir    = escHtml(p.getDireccion());
            String hora   = (p.getHoraLlegadaEstimada() != null) ? "<br>Llegada: " + p.getHoraLlegadaEstimada() : "";
            String estado = (paradaActiva >= 0 && i < paradaActiva) ? " ✓"
                    : (paradaActiva >= 0 && i == paradaActiva) ? " ▶" : "";
            String popup  = "<b>" + (i + 1) + ". " + nombre + estado + "</b>"
                    + (dir.isEmpty() ? "" : "<br>" + dir) + hora
                    + "<br><button onclick='irHacia(" + p.getLatitud() + "," + p.getLongitud() + ")'>\uD83E\uDDED Ir hacia all\u00ed</button>";

            sb.append("L.marker([").append(p.getLatitud()).append(",").append(p.getLongitud()).append("],{")
                    .append("icon:L.divIcon({className:'',")
                    .append("html:'<div class=\"nm\" style=\"background:").append(color).append(";\">").append(i + 1).append("</div>',")
                    .append("iconSize:[30,30],iconAnchor:[15,15],popupAnchor:[0,-15]})})")
                    .append(".addTo(map)")
                    .append(".bindPopup(\"").append(popup).append("\");");
        }

        sb.append("</script></body></html>");
        return sb.toString();
    }

    private void empezarRuta() {
        if (ultimaRuta == null || ultimaRuta.isEmpty()) {
            mostrarAlerta("Error", "No hay ruta calculada");
            return;
        }
        paradaActualIndex = 0;
        btnMarcarCompletada.setDisable(false);
        actualizarVistaRuta();
        abrirMapaLeaflet(paradaActualIndex); // Abre mapa mostrando la primera parada activa
    }

    private void marcarParadaCompletada() {
        paradaActualIndex++;
        if (paradaActualIndex >= ultimaRuta.size()) {
            paradaActualIndex = ultimaRuta.size(); // todas completadas
            lblParadaActual.setText("✓  ¡Ruta completada! Regresando al almacén.");
            lblParadaActual.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            btnMarcarCompletada.setDisable(true);
            tablaParadas.refresh();
        } else {
            actualizarVistaRuta();
        }
    }

    private void actualizarVistaRuta() {
        tablaParadas.refresh(); // fuerza el redibujado de colores de filas
        if (paradaActualIndex < ultimaRuta.size()) {
            Parada actual = ultimaRuta.get(paradaActualIndex);
            String texto = (paradaActualIndex == 0)
                    ? "📦  Saliendo de: " + actual.getNombre()
                    : "📍  Parada " + (paradaActualIndex + 1) + " de " + ultimaRuta.size() + ":  " + actual.getNombre();
            lblParadaActual.setText(texto);
            lblParadaActual.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            tablaParadas.scrollTo(paradaActualIndex);
        }
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;").replace("\n", " ");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
