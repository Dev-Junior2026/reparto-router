package com.luispacheco.reparto.ui;

import com.luispacheco.reparto.model.FilaImportada;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ventana modal que muestra las filas extraídas de un PDF de reparto,
 * permite marcar/desmarcar cuáles importar, y devuelve la selección final.
 */
public class VentanaPrevisualizacionPdf {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Envuelve una FilaImportada con una propiedad observable "seleccionada",
     * necesaria para que la columna de checkbox de la tabla pueda enlazarse (bind)
     * a cada fila individualmente.
     */
    private static class FilaSeleccionable {
        final FilaImportada fila;
        final SimpleBooleanProperty seleccionada = new SimpleBooleanProperty(true);

        FilaSeleccionable(FilaImportada fila) {
            this.fila = fila;
        }
    }

    /**
     * Muestra la ventana y BLOQUEA la ejecución (showAndWait) hasta que el usuario
     * confirme o cancele. Devuelve la lista de filas que el usuario dejó marcadas,
     * o una lista vacía si canceló.
     */
    public static List<FilaImportada> mostrar(Stage ventanaPadre, List<FilaImportada> filas) {

        List<FilaSeleccionable> filasSeleccionables = filas.stream()
                .map(FilaSeleccionable::new)
                .collect(Collectors.toList());

        TableView<FilaSeleccionable> tabla = new TableView<>();
        tabla.setEditable(true);
        tabla.getItems().addAll(filasSeleccionables);

        // Columna de checkbox: se enlaza a la propiedad "seleccionada" de cada fila
        TableColumn<FilaSeleccionable, Boolean> colCheck = new TableColumn<>("Importar");
        colCheck.setCellValueFactory(data -> data.getValue().seleccionada);
        colCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colCheck));
        colCheck.setEditable(true);
        colCheck.setPrefWidth(70);

        TableColumn<FilaSeleccionable, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().fila.getNombre()));
        colNombre.setPrefWidth(160);

        TableColumn<FilaSeleccionable, String> colCalle = new TableColumn<>("Calle");
        colCalle.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().fila.getCalle()));
        colCalle.setPrefWidth(220);

        TableColumn<FilaSeleccionable, String> colCP = new TableColumn<>("C.P.");
        colCP.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().fila.getCodigoPostal()));
        colCP.setPrefWidth(60);

        TableColumn<FilaSeleccionable, String> colPoblacion = new TableColumn<>("Población");
        colPoblacion.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().fila.getPoblacion()));
        colPoblacion.setPrefWidth(100);

        TableColumn<FilaSeleccionable, String> colHorario = new TableColumn<>("Horario");
        colHorario.setCellValueFactory(data -> {
            FilaImportada f = data.getValue().fila;
            String texto = f.getHoraApertura().format(FORMATO_HORA) + " - " + f.getHoraCierre().format(FORMATO_HORA);
            return new javafx.beans.property.SimpleStringProperty(texto);
        });
        colHorario.setPrefWidth(110);

        TableColumn<FilaSeleccionable, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().fila.isHorarioDetectado() ? "\u2713 Detectado" : "\u26a0 Por defecto"));
        colEstado.setPrefWidth(110);

        tabla.getColumns().addAll(colCheck, colNombre, colCalle, colCP, colPoblacion, colHorario, colEstado);

        // Resalta en naranja claro las filas donde NO se detectó horario en el PDF,
        // para que el usuario sepa cuáles conviene revisar/editar después a mano.
        tabla.setRowFactory(tv -> new TableRow<FilaSeleccionable>() {
            @Override
            protected void updateItem(FilaSeleccionable item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (!item.fila.isHorarioDetectado()) {
                    setStyle("-fx-background-color: #fdebd0;");
                } else {
                    setStyle("");
                }
            }
        });

        Label titulo = new Label("Se encontraron " + filas.size() + " paradas en el PDF. Revisa y desmarca las que no quieras importar:");
        titulo.setStyle("-fx-font-weight: bold; -fx-padding: 10 10 0 10;");

        Label leyenda = new Label("\u26a0 Naranja = no se detectó un horario válido en el PDF; se asignó 9:00-20:00 por defecto.");
        leyenda.setStyle("-fx-padding: 0 10 10 10; -fx-text-fill: #7d6608;");

        Button btnSeleccionarTodo = new Button("Seleccionar todo");
        btnSeleccionarTodo.setOnAction(e ->
                filasSeleccionables.forEach(f -> f.seleccionada.set(true)));

        Button btnDeseleccionarTodo = new Button("Deseleccionar todo");
        btnDeseleccionarTodo.setOnAction(e ->
                filasSeleccionables.forEach(f -> f.seleccionada.set(false)));

        HBox botonesSeleccion = new HBox(10, btnSeleccionarTodo, btnDeseleccionarTodo);
        botonesSeleccion.setPadding(new Insets(0, 10, 10, 10));

        // Resultado final: se rellena solo si el usuario pulsa "Confirmar".
        // Es un array de un elemento (en vez de una variable normal) porque los
        // lambdas de los botones necesitan capturar una referencia "efectivamente final".
        List<FilaImportada>[] resultado = new List[]{new ArrayList<>()};

        Stage ventana = new Stage();
        ventana.initOwner(ventanaPadre);
        ventana.initModality(Modality.WINDOW_MODAL); // bloquea la ventana principal mientras esta esté abierta
        ventana.setTitle("Previsualización de importación PDF");

        Button btnConfirmar = new Button("Confirmar selección");
        btnConfirmar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 8 16;");
        btnConfirmar.setOnAction(e -> {
            resultado[0] = filasSeleccionables.stream()
                    .filter(f -> f.seleccionada.get())
                    .map(f -> f.fila)
                    .collect(Collectors.toList());
            ventana.close();
        });

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> {
            resultado[0] = new ArrayList<>(); // lista vacía = "no importar nada"
            ventana.close();
        });

        HBox botonesFinales = new HBox(10, btnConfirmar, btnCancelar);
        botonesFinales.setPadding(new Insets(10));

        VBox contenedorSuperior = new VBox(titulo, leyenda, botonesSeleccion);

        BorderPane raiz = new BorderPane();
        raiz.setTop(contenedorSuperior);
        raiz.setCenter(tabla);
        raiz.setBottom(botonesFinales);

        ventana.setScene(new Scene(raiz, 950, 500));
        ventana.showAndWait(); // BLOQUEA aquí hasta que se cierre la ventana

        return resultado[0];
    }
}
