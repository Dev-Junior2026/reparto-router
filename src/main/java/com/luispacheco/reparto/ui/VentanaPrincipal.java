package com.luispacheco.reparto.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.model.Ruta;
import com.luispacheco.reparto.model.ConfiguracionReparto;
import com.luispacheco.reparto.service.EnrutadorService;

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

    // Panel derecho: resultados
    private TableView<Parada> tablaParadas;
    private Label lblDistancia;
    private Label lblTiempo;
    private Label lblHoraFin;

    @Override
    public void start(Stage primaryStage) {
        enrutadorService = new EnrutadorService();
        paradasIngresadas = FXCollections.observableArrayList();
        almacenGlobal = null;

        // --- PANEL IZQUIERDO: Entrada de paradas ---
        VBox panelIzquierdo = crearPanelEntrada();

        // --- PANEL DERECHO: Resultados ---
        VBox panelDerecho = crearPanelResultados();

        // --- SPLIT PANE ---
        SplitPane splitPane = new SplitPane(panelIzquierdo, panelDerecho);
        splitPane.setDividerPositions(0.4);

        Scene escena = new Scene(splitPane, 1200, 600);
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

        // Formulario para almacén (solo se ingresa una vez)
        Label lblAlmacen = new Label("Almacén (Parada 1)");
        lblAlmacen.setStyle("-fx-font-weight: bold");

        TextField txtNombreAlmacen = new TextField();
        txtNombreAlmacen.setPromptText("Ej: Almacén Central");

        TextField txtDireccionAlmacen = new TextField();
        txtDireccionAlmacen.setPromptText("Ej: Calle Mayor 1");

        Spinner<Double> spinLatitudAlmacen = new Spinner<>(40.0, 41.0, 40.4168, 0.0001);
        spinLatitudAlmacen.setPrefWidth(100);
        Spinner<Double> spinLongitudAlmacen = new Spinner<>(-4.0, -3.0, -3.7038, 0.0001);
        spinLongitudAlmacen.setPrefWidth(100);

        Button btnCrearAlmacen = new Button("Crear Almacén");
        btnCrearAlmacen.setOnAction(e -> crearAlmacen(txtNombreAlmacen.getText(), txtDireccionAlmacen.getText(),
                spinLatitudAlmacen.getValue(), spinLongitudAlmacen.getValue()));

        HBox almacenBox = new HBox(5, new Label("Nombre:"), txtNombreAlmacen, new Label("Dir:"), txtDireccionAlmacen,
                new Label("Lat:"), spinLatitudAlmacen, new Label("Lon:"), spinLongitudAlmacen, btnCrearAlmacen);
        almacenBox.setStyle("-fx-border-bottom: 1px solid #ccc; -fx-padding: 10");

        // Formulario para clientes
        Label lblClientes = new Label("Añadir Clientes");
        lblClientes.setStyle("-fx-font-weight: bold");

        txtNombre = new TextField();
        txtNombre.setPromptText("Nombre cliente");

        txtDireccion = new TextField();
        txtDireccion.setPromptText("Dirección");

        spinLatitud = new Spinner<>(40.0, 41.0, 40.42, 0.0001);
        spinLatitud.setPrefWidth(90);
        spinLongitud = new Spinner<>(-4.0, -3.0, -3.70, 0.0001);
        spinLongitud.setPrefWidth(90);

        spinHoraApertura = new Spinner<>(0, 23, 9);
        spinHoraApertura.setPrefWidth(60);
        spinMinutoApertura = new Spinner<>(0, 59, 0);
        spinMinutoApertura.setPrefWidth(60);
        spinHoraCierre = new Spinner<>(0, 23, 13);
        spinHoraCierre.setPrefWidth(60);
        spinMinutoCierre = new Spinner<>(0, 59, 0);
        spinMinutoCierre.setPrefWidth(60);

        Button btnAnadirParada = new Button("Añadir Parada");
        btnAnadirParada.setOnAction(e -> anadirParada());

        HBox formularioBox = new HBox(5,
                new Label("Nombre:"), txtNombre,
                new Label("Dir:"), txtDireccion,
                new Label("Lat:"), spinLatitud,
                new Label("Lon:"), spinLongitud,
                new Label("Abre:"), spinHoraApertura, new Label(":"), spinMinutoApertura,
                new Label("Cierra:"), spinHoraCierre, new Label(":"), spinMinutoCierre,
                btnAnadirParada);

        // Tabla de paradas ingresadas
        tablaParadasIngresadas = new TableView<>();
        tablaParadasIngresadas.setItems(paradasIngresadas);

        TableColumn<Parada, String> col1 = new TableColumn<>("Nombre");
        col1.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<Parada, String> col2 = new TableColumn<>("Dirección");
        col2.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        tablaParadasIngresadas.getColumns().addAll(col1, col2);
        tablaParadasIngresadas.setPrefHeight(150);

        Button btnCalcularRuta = new Button("Calcular Ruta");
        btnCalcularRuta.setPrefWidth(150);
        btnCalcularRuta.setStyle("-fx-font-size: 12; -fx-padding: 8");
        btnCalcularRuta.setOnAction(e -> calcularRuta());

        panel.getChildren().addAll(
                titulo,
                lblAlmacen, almacenBox,
                lblClientes, formularioBox,
                new Label("Paradas ingresadas:"), tablaParadasIngresadas,
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

        // Tabla de resultados
        tablaParadas = new TableView<>();

        TableColumn<Parada, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(120);

        TableColumn<Parada, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colDireccion.setPrefWidth(150);

        TableColumn<Parada, LocalTime> colHoraLlegada = new TableColumn<>("Hora Llegada");
        colHoraLlegada.setCellValueFactory(new PropertyValueFactory<>("horaLlegadaEstimada"));
        colHoraLlegada.setCellFactory(column -> new TableCell<Parada, LocalTime>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "-" : item.toString());
            }
        });
        colHoraLlegada.setPrefWidth(100);

        tablaParadas.getColumns().addAll(colNombre, colDireccion, colHoraLlegada);

        // Panel de información
        lblDistancia = new Label("Distancia total: -");
        lblTiempo = new Label("Tiempo total: -");
        lblHoraFin = new Label("Hora fin: -");

        HBox panelInfo = new HBox(20);
        panelInfo.setPadding(new Insets(10));
        panelInfo.setStyle("-fx-border-top: 1px solid #ccc");
        panelInfo.getChildren().addAll(lblDistancia, lblTiempo, lblHoraFin);

        panel.getChildren().addAll(titulo, tablaParadas, panelInfo);
        VBox.setVgrow(tablaParadas, javafx.scene.layout.Priority.ALWAYS);

        return panel;
    }

    private void crearAlmacen(String nombre, String direccion, double lat, double lon) {
        if (nombre.isEmpty() || direccion.isEmpty()) {
            mostrarAlerta("Error", "Completa nombre y dirección del almacén");
            return;
        }
        almacenGlobal = new Parada(1, nombre, direccion, lat, lon, LocalTime.of(8, 0), LocalTime.of(20, 0));
        mostrarAlerta("Éxito", "Almacén creado: " + nombre);
    }

    private void anadirParada() {
        if (almacenGlobal == null) {
            mostrarAlerta("Error", "Crea el almacén primero");
            return;
        }

        String nombre = txtNombre.getText();
        String direccion = txtDireccion.getText();

        if (nombre.isEmpty() || direccion.isEmpty()) {
            mostrarAlerta("Error", "Completa nombre y dirección");
            return;
        }

        double lat = spinLatitud.getValue();
        double lon = spinLongitud.getValue();
        int horaApertura = spinHoraApertura.getValue();
        int minutoApertura = spinMinutoApertura.getValue();
        int horaCierre = spinHoraCierre.getValue();
        int minutoCierre = spinMinutoCierre.getValue();

        Parada nueva = new Parada(
                paradasIngresadas.size() + 2,  // numero = 2, 3, 4... (1 es el almacén)
                nombre, direccion, lat, lon,
                LocalTime.of(horaApertura, minutoApertura),
                LocalTime.of(horaCierre, minutoCierre)
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

        // Construir lista de paradas con el almacén al principio
        List<Parada> todasLasParadas = new ArrayList<>();
        todasLasParadas.add(almacenGlobal);
        todasLasParadas.addAll(paradasIngresadas);

        ConfiguracionReparto config = new ConfiguracionReparto(almacenGlobal, LocalTime.of(8, 0));

        // Ejecutar algoritmo
        Ruta ruta = enrutadorService.calcularRuta(todasLasParadas, config);

        // Mostrar en tabla
        tablaParadas.getItems().clear();
        tablaParadas.getItems().addAll(ruta.getParadasOrdenadas());

        // Actualizar panel de información
        lblDistancia.setText(String.format("Distancia total: %.2f km", ruta.getDistanciaTotalKm()));
        lblTiempo.setText("Tiempo total: " + ruta.getTiempoTotalEstimado());
        lblHoraFin.setText("Hora fin: " + ruta.getHoraFinEstimada());
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