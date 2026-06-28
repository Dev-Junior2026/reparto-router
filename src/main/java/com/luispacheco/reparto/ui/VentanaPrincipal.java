package com.luispacheco.reparto.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
    private TableView<Parada> tablaParadas;

    @Override
    public void start(Stage primaryStage) {
        enrutadorService = new EnrutadorService();

        // --- Crear la tabla ---
        tablaParadas = new TableView<>();

        // Columna: Nombre
        TableColumn<Parada, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(150);

        // Columna: Dirección
        TableColumn<Parada, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colDireccion.setPrefWidth(200);

        // Columna: Hora de llegada
        TableColumn<Parada, LocalTime> colHoraLlegada = new TableColumn<>("Hora Llegada");
        colHoraLlegada.setCellValueFactory(new PropertyValueFactory<>("horaLlegadaEstimada"));
        colHoraLlegada.setPrefWidth(120);

        tablaParadas.getColumns().addAll(colNombre, colDireccion, colHoraLlegada);

        // --- Botón para calcular ruta ---
        Button btnCalcular = new Button("Calcular Ruta");
        btnCalcular.setOnAction(e -> calcularRuta());

        HBox panelBotones = new HBox(10);
        panelBotones.setPadding(new Insets(10));
        panelBotones.getChildren().add(btnCalcular);

        // --- Layout principal ---
        BorderPane raiz = new BorderPane();
        raiz.setTop(panelBotones);
        raiz.setCenter(tablaParadas);

        Scene escena = new Scene(raiz, 600, 400);
        primaryStage.setTitle("Reparto Router");
        primaryStage.setScene(escena);
        primaryStage.show();
    }

    private void calcularRuta() {
        // Datos de prueba (iguales a Main.java)
        Parada almacen = new Parada(1, "Almacen Central", "Madrid centro", 40.4168, -3.7038,
                LocalTime.of(8, 0), LocalTime.of(20, 0));

        Parada clienteA = new Parada(2, "Cliente A", "Chamartin", 40.4500, -3.6900,
                LocalTime.of(9, 0), LocalTime.of(13, 0));

        Parada clienteB = new Parada(3, "Cliente B", "Carabanchel", 40.3800, -3.7200,
                LocalTime.of(9, 0), LocalTime.of(18, 0));

        Parada clienteC = new Parada(4, "Cliente C", "Ciudad Lineal", 40.4300, -3.6500,
                LocalTime.of(10, 0), LocalTime.of(14, 0));

        List<Parada> paradas = new ArrayList<>();
        paradas.add(almacen);
        paradas.add(clienteA);
        paradas.add(clienteB);
        paradas.add(clienteC);

        ConfiguracionReparto config = new ConfiguracionReparto(almacen, LocalTime.of(8, 0));

        // Ejecutar algoritmo
        Ruta ruta = enrutadorService.calcularRuta(paradas, config);

        // Mostrar en tabla
        tablaParadas.getItems().clear();
        tablaParadas.getItems().addAll(ruta.getParadasOrdenadas());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
