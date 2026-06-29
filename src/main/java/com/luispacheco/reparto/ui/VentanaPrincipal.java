package com.luispacheco.reparto.ui;

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

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class VentanaPrincipal extends Application {

    private EnrutadorService enrutadorService;

    // Panel izquierdo: entrada de paradas
    private TextField txtNombre;
    private TextField txtDireccion;
    private Spinner<Double> spinLatitud;
    private Spinner<Double> spinLongitud;
    private Spinner<Integer> spinHoraApertura;
    private Spinner<Integer> spinMinutoApertura;
    private Spinner<Integer> spinHoraCierre;
    private Spinner<Integer> spinMinutoCierre;
    private TableView<Parada> tablaParadasIngresadas;
    private ObservableList<Parada> paradasIngresadas;
    private Parada almacenGlobal;
    private Button btnCalcularRuta;

    // Panel derecho: resultados
    private TableView<Parada> tablaParadas;
    private Label lblDistancia;
    private Label lblTiempo;
    private Label lblHoraFin;
    private Button btnVerEnMapa;

    // Última ruta calculada (para el botón Ver en mapa)
    private List<Parada> ultimaRuta;

    @Override
    public void start(Stage primaryStage) {
        enrutadorService = new EnrutadorService();
        paradasIngresadas = FXCollections.observableArrayList();
        almacenGlobal = null;
        ultimaRuta = new ArrayList<>();

        // --- PANEL IZQUIERDO: Entrada de paradas ---
        VBox panelIzquierdo = crearPanelEntrada();

        // --- PANEL DERECHO: Resultados ---
        VBox panelDerecho = crearPanelResultados();

        // --- SPLIT PANE ---
        SplitPane splitPane = new SplitPane(panelIzquierdo, panelDerecho);
        splitPane.setDividerPositions(0.45);

        Scene escena = new Scene(splitPane, 1400, 700);
        primaryStage.setTitle("Reparto Router");
        primaryStage.setScene(escena);
        primaryStage.show();
    }

    private VBox crearPanelEntrada() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // Título
        Label titulo = new Label("Gestión de Paradas");
        titulo.setStyle("-fx-font-size: 14; -fx-font-weight: bold");

        // === FORMULARIO PARA ALMACÉN ===
        Label lblAlmacen = new Label("Almacén (Parada 1)");
        lblAlmacen.setStyle("-fx-font-weight: bold");

        TextField txtNombreAlmacen = new TextField();
        txtNombreAlmacen.setPromptText("Ej: Almacén Central");
        txtNombreAlmacen.setPrefWidth(150);

        TextField txtDireccionAlmacen = new TextField();
        txtDireccionAlmacen.setPromptText("Ej: Calle Mayor 1");
        txtDireccionAlmacen.setPrefWidth(180);

        Spinner<Double> spinLatitudAlmacen = new Spinner<>(40.0, 41.0, 40.4168, 0.0001);
        spinLatitudAlmacen.setPrefWidth(100);
        Spinner<Double> spinLongitudAlmacen = new Spinner<>(-4.0, -3.0, -3.7038, 0.0001);
        spinLongitudAlmacen.setPrefWidth(100);

        Button btnCrearAlmacen = new Button("Crear Almacén");
        btnCrearAlmacen.setOnAction(e -> crearAlmacen(
                txtNombreAlmacen.getText(),
                txtDireccionAlmacen.getText(),
                spinLatitudAlmacen.getValue(),
                spinLongitudAlmacen.getValue()
        ));

        HBox almacenBox = new HBox(8,
                new Label("Nombre:"), txtNombreAlmacen,
                new Label("Dir:"), txtDireccionAlmacen,
                new Label("Lat:"), spinLatitudAlmacen,
                new Label("Lon:"), spinLongitudAlmacen,
                btnCrearAlmacen
        );
        almacenBox.setStyle("-fx-border-bottom: 2px solid #ccc; -fx-padding: 10");

        // === FORMULARIO PARA CLIENTES ===
        Label lblClientes = new Label("Añadir Clientes");
        lblClientes.setStyle("-fx-font-weight: bold");

        txtNombre = new TextField();
        txtNombre.setPromptText("Nombre cliente");
        txtNombre.setPrefWidth(140);

        txtDireccion = new TextField();
        txtDireccion.setPromptText("Dirección");
        txtDireccion.setPrefWidth(160);

        spinLatitud = new Spinner<>(40.0, 41.0, 40.42, 0.0001);
        spinLatitud.setPrefWidth(85);
        spinLongitud = new Spinner<>(-4.0, -3.0, -3.70, 0.0001);
        spinLongitud.setPrefWidth(85);

        spinHoraApertura = new Spinner<>(0, 23, 9);
        spinHoraApertura.setPrefWidth(55);
        spinMinutoApertura = new Spinner<>(0, 59, 0);
        spinMinutoApertura.setPrefWidth(55);
        spinHoraCierre = new Spinner<>(0, 23, 13);
        spinHoraCierre.setPrefWidth(55);
        spinMinutoCierre = new Spinner<>(0, 59, 0);
        spinMinutoCierre.setPrefWidth(55);

        Button btnAnadirParada = new Button("Añadir Parada");
        btnAnadirParada.setOnAction(e -> anadirParada());

        HBox formularioBox = new HBox(5);
        formularioBox.setSpacing(5);
        formularioBox.getChildren().addAll(
                new Label("Nombre:"), txtNombre,
                new Label("Dir:"), txtDireccion,
                new Label("Lat:"), spinLatitud,
                new Label("Lon:"), spinLongitud,
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
        col1.setPrefWidth(140);

        TableColumn<Parada, String> col2 = new TableColumn<>("Dirección");
        col2.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        col2.setPrefWidth(160);

        // Columna de acción: Eliminar
        TableColumn<Parada, Void> colEliminar = new TableColumn<>("Acción");
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
        colEliminar.setPrefWidth(80);

        tablaParadasIngresadas.getColumns().addAll(col1, col2, colEliminar);
        tablaParadasIngresadas.setPrefHeight(180);

        // === BOTÓN CALCULAR RUTA ===
        btnCalcularRuta = new Button("Calcular Ruta");
        btnCalcularRuta.setPrefWidth(150);
        btnCalcularRuta.setStyle("-fx-font-size: 12; -fx-padding: 8");
        btnCalcularRuta.setDisable(true);
        btnCalcularRuta.setOnAction(e -> calcularRuta());

        // === AGREGAR TODO AL PANEL ===
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

        // Título
        Label titulo = new Label("Ruta Calculada");
        titulo.setStyle("-fx-font-size: 14; -fx-font-weight: bold");

        // === TABLA DE RESULTADOS ===
        tablaParadas = new TableView<>();

        TableColumn<Parada, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(140);

        TableColumn<Parada, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colDireccion.setPrefWidth(180);

        TableColumn<Parada, LocalTime> colHoraLlegada = new TableColumn<>("Hora Llegada");
        colHoraLlegada.setCellValueFactory(new PropertyValueFactory<>("horaLlegadaEstimada"));
        colHoraLlegada.setCellFactory(column -> new TableCell<Parada, LocalTime>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "-" : item.toString());
            }
        });
        colHoraLlegada.setPrefWidth(120);

        tablaParadas.getColumns().addAll(colNombre, colDireccion, colHoraLlegada);
        VBox.setVgrow(tablaParadas, Priority.ALWAYS);

        // === BOTÓN VER EN MAPA ===
        btnVerEnMapa = new Button("🗺 Ver en mapa (OpenStreetMap)");
        btnVerEnMapa.setPrefWidth(250);
        btnVerEnMapa.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        btnVerEnMapa.setDisable(true); // Se activa tras calcular la ruta
        btnVerEnMapa.setOnAction(e -> abrirEnMapa());

        // === PANEL DE INFORMACIÓN ===
        lblDistancia = new Label("Distancia total: -");
        lblTiempo = new Label("Tiempo total: -");
        lblHoraFin = new Label("Hora fin: -");

        HBox panelInfo = new HBox(20);
        panelInfo.setPadding(new Insets(10));
        panelInfo.setStyle("-fx-border-top: 2px solid #ccc");
        panelInfo.getChildren().addAll(lblDistancia, lblTiempo, lblHoraFin);

        panel.getChildren().addAll(titulo, tablaParadas, btnVerEnMapa, panelInfo);

        return panel;
    }

    private void crearAlmacen(String nombre, String direccion, double lat, double lon) {
        if (nombre.trim().isEmpty() || direccion.trim().isEmpty()) {
            mostrarAlerta("Error", "Completa nombre y dirección del almacén");
            return;
        }

        if (lat < 40.0 || lat > 41.0) {
            mostrarAlerta("Error", "Latitud debe estar entre 40.0 y 41.0");
            return;
        }
        if (lon < -4.0 || lon > -3.0) {
            mostrarAlerta("Error", "Longitud debe estar entre -4.0 y -3.0");
            return;
        }

        almacenGlobal = new Parada(1, nombre, direccion, lat, lon,
                LocalTime.of(8, 0), LocalTime.of(20, 0));
        btnCalcularRuta.setDisable(false);
        mostrarAlerta("Éxito", "Almacén creado: " + nombre);
    }

    private void anadirParada() {
        if (almacenGlobal == null) {
            mostrarAlerta("Error", "Crea el almacén primero");
            return;
        }

        String nombre = txtNombre.getText().trim();
        String direccion = txtDireccion.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAlerta("Error", "Ingresa un nombre para la parada");
            return;
        }
        if (direccion.isEmpty()) {
            mostrarAlerta("Error", "Ingresa una dirección");
            return;
        }

        double lat = spinLatitud.getValue();
        double lon = spinLongitud.getValue();

        if (lat < 40.0 || lat > 41.0) {
            mostrarAlerta("Error", "Latitud debe estar entre 40.0 y 41.0");
            return;
        }
        if (lon < -4.0 || lon > -3.0) {
            mostrarAlerta("Error", "Longitud debe estar entre -4.0 y -3.0");
            return;
        }

        int horaApertura = spinHoraApertura.getValue();
        int minutoApertura = spinMinutoApertura.getValue();
        int horaCierre = spinHoraCierre.getValue();
        int minutoCierre = spinMinutoCierre.getValue();

        LocalTime abre = LocalTime.of(horaApertura, minutoApertura);
        LocalTime cierra = LocalTime.of(horaCierre, minutoCierre);
        if (cierra.isBefore(abre) || cierra.equals(abre)) {
            mostrarAlerta("Error", "La hora de cierre debe ser después de apertura");
            return;
        }

        Parada nueva = new Parada(
                paradasIngresadas.size() + 2,
                nombre, direccion, lat, lon,
                abre, cierra
        );

        paradasIngresadas.add(nueva);

        // Limpiar formulario
        txtNombre.clear();
        txtDireccion.clear();
        spinLatitud.getValueFactory().setValue(40.42);
        spinLongitud.getValueFactory().setValue(-3.70);
        spinHoraApertura.getValueFactory().setValue(9);
        spinMinutoApertura.getValueFactory().setValue(0);
        spinHoraCierre.getValueFactory().setValue(13);
        spinMinutoCierre.getValueFactory().setValue(0);

        mostrarAlerta("Éxito", "Parada añadida: " + nombre);
    }

    private void calcularRuta() {
        if (almacenGlobal == null) {
            mostrarAlerta("Error", "Crea el almacén primero");
            return;
        }

        if (paradasIngresadas.isEmpty()) {
            mostrarAlerta("Error", "Añade al menos una parada");
            return;
        }

        List<Parada> todasLasParadas = new ArrayList<>();
        todasLasParadas.add(almacenGlobal);
        todasLasParadas.addAll(paradasIngresadas);

        ConfiguracionReparto config = new ConfiguracionReparto(almacenGlobal, LocalTime.of(8, 0));

        Ruta ruta = enrutadorService.calcularRuta(todasLasParadas, config);

        // Mostrar en tabla
        tablaParadas.getItems().clear();
        tablaParadas.getItems().addAll(ruta.getParadasOrdenadas());

        // Guardar ruta para el botón de mapa
        ultimaRuta = new ArrayList<>(ruta.getParadasOrdenadas());

        // Actualizar panel de información
        lblDistancia.setText(String.format("Distancia total: %.2f km", ruta.getDistanciaTotalKm()));
        lblTiempo.setText("Tiempo total: " + ruta.getTiempoTotalEstimado());
        lblHoraFin.setText("Hora fin: " + ruta.getHoraFinEstimada());

        // Activar botón de mapa
        btnVerEnMapa.setDisable(false);
    }

    /**
     * Construye una URL de OpenStreetMap con todas las paradas como waypoints
     * y la abre en el navegador predeterminado del sistema.
     *
     * Formato URL: https://www.openstreetmap.org/directions?engine=fossgis_osrm_car
     *   &route=lat1,lon1;lat2,lon2;...
     */
    private void abrirEnMapa() {
        if (ultimaRuta == null || ultimaRuta.isEmpty()) {
            mostrarAlerta("Error", "No hay ruta calculada");
            return;
        }

        try {
            // Construir la cadena de waypoints: lat,lon;lat,lon;...
            StringBuilder waypoints = new StringBuilder();
            for (int i = 0; i < ultimaRuta.size(); i++) {
                Parada p = ultimaRuta.get(i);
                if (i > 0) waypoints.append(";");
                waypoints.append(p.getLatitud()).append(",").append(p.getLongitud());
            }

            // Añadir el almacén al final para cerrar el ciclo de reparto
            Parada inicio = ultimaRuta.get(0);
            waypoints.append(";").append(inicio.getLatitud()).append(",").append(inicio.getLongitud());

            String url = "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route="
                    + waypoints.toString();

            // Abrir en el navegador del sistema
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                mostrarAlerta("Error", "Tu sistema no soporta abrir el navegador automáticamente.\n\nCopia esta URL:\n" + url);
            }

        } catch (Exception ex) {
            mostrarAlerta("Error", "No se pudo abrir el mapa: " + ex.getMessage());
        }
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