package com.luispacheco.reparto.ui;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;

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
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.scene.control.ProgressBar;
import javafx.geometry.Pos;
import javafx.stage.StageStyle;
import javafx.stage.Modality;

import com.luispacheco.reparto.model.Parada;
import com.luispacheco.reparto.model.Ruta;
import com.luispacheco.reparto.model.ConfiguracionReparto;
import com.luispacheco.reparto.service.EnrutadorService;
import com.luispacheco.reparto.service.GeocodificacionService;
import com.luispacheco.reparto.model.FilaImportada;
import com.luispacheco.reparto.service.ImportadorPdfService;

import java.awt.Desktop;

import java.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;

public class VentanaPrincipal extends Application {

    private EnrutadorService enrutadorService;
    private GeocodificacionService geocodificacionService;
    private Stage primaryStage;
    private ImportadorPdfService importadorPdfService;

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
    private Button btnAnadirParada;
    private Button btnCrearAlmacen;
    private Button btnImportarPdf;

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

    // Contador que solo incrementa: garantiza números de parada únicos aunque
    // se eliminen paradas de en medio de la lista. El almacén siempre es el 1.
    private int siguienteNumeroParada;

    private HttpServer servidorMapa;
    private int puertoMapa;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        enrutadorService = new EnrutadorService();
        geocodificacionService = new GeocodificacionService();
        importadorPdfService = new ImportadorPdfService();
        paradasIngresadas = FXCollections.observableArrayList();
        almacenGlobal = null;
        ultimaRuta = new ArrayList<>();
        paradaActualIndex = -1;
        siguienteNumeroParada = 2; // el almacén ocupa el número 1
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
        spinHoraInicioRuta.setEditable(true);
        confirmarTextoAlPerderFoco(spinHoraInicioRuta);
        spinMinutoInicioRuta = new Spinner<>(0, 59, 0);
        spinMinutoInicioRuta.setPrefWidth(55);
        spinMinutoInicioRuta.setEditable(true);
        confirmarTextoAlPerderFoco(spinMinutoInicioRuta);

        btnCrearAlmacen = new Button("Crear Almac\u00e9n");
        btnCrearAlmacen.setOnAction(e -> crearAlmacen());

        HBox almacenBox = new HBox(6);
        almacenBox.getChildren().addAll(
                new Label("Nombre:"), txtNombreAlmacen,
                new Label("Calle:"), txtCalleAlmacen,
                new Label("CP:"), txtCodigoPostalAlmacen,
                new Label("Pob:"), txtPoblacionAlmacen,
                new Label("Inicio ruta:"), spinHoraInicioRuta, new Label(":"), spinMinutoInicioRuta,
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
        spinHoraApertura.setEditable(true);
        confirmarTextoAlPerderFoco(spinHoraApertura);
        spinMinutoApertura = new Spinner<>(0, 59, 0);
        spinMinutoApertura.setPrefWidth(50);
        spinMinutoApertura.setEditable(true);
        confirmarTextoAlPerderFoco(spinMinutoApertura);
        spinHoraCierre = new Spinner<>(0, 23, 13);
        spinHoraCierre.setPrefWidth(50);
        spinHoraCierre.setEditable(true);
        confirmarTextoAlPerderFoco(spinHoraCierre);
        spinMinutoCierre = new Spinner<>(0, 59, 0);
        spinMinutoCierre.setPrefWidth(50);
        spinMinutoCierre.setEditable(true);
        confirmarTextoAlPerderFoco(spinMinutoCierre);

        btnAnadirParada = new Button("A\u00f1adir Parada");
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

        btnImportarPdf = new Button("\uD83D\uDCC4 Importar desde PDF");
        btnImportarPdf.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #9b59b6; -fx-text-fill: white;");
        btnImportarPdf.setOnAction(e -> importarDesdePdf());

        Label lblParadasIngresadas = new Label("Paradas ingresadas:");
        lblParadasIngresadas.setStyle("-fx-font-weight: bold");

        tablaParadasIngresadas = new TableView<>();
        tablaParadasIngresadas.setItems(paradasIngresadas);

        TableColumn<Parada, String> col1 = new TableColumn<>("Nombre");
        col1.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        col1.setPrefWidth(130);

        TableColumn<Parada, String> col2 = new TableColumn<>("Direcci\u00f3n");
        col2.setCellValueFactory(new PropertyValueFactory<>("calle"));
        col2.setPrefWidth(200);

        TableColumn<Parada, String> colCP = new TableColumn<>("C.P.");
        colCP.setCellValueFactory(new PropertyValueFactory<>("codigoPostal"));
        colCP.setPrefWidth(60);

        TableColumn<Parada, String> colPoblacion = new TableColumn<>("Poblaci\u00f3n");
        colPoblacion.setCellValueFactory(new PropertyValueFactory<>("poblacion"));
        colPoblacion.setPrefWidth(100);

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

        tablaParadasIngresadas.getColumns().addAll(col1, col2, colCP, colPoblacion, colHorario, colEliminar);
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
                btnImportarPdf,
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

        // El almac\u00e9n no tiene horario propio como restricci\u00f3n: la \u00fanica hora que
        // importa es "Inicio ruta", que sirve de referencia para calcular las horas
        // de llegada al resto de paradas. Por eso aqu\u00ed se le da una ventana abierta
        // todo el d\u00eda, para que nunca bloquee el c\u00e1lculo de la ruta.
        LocalTime aperturaAlmacen = LocalTime.MIN;
        LocalTime cierreAlmacen = LocalTime.of(23, 59);

        String direccionCompleta = calle + ", " + cp + " " + poblacion;

        btnCrearAlmacen.setDisable(true);

        Task<double[]> tareaGeocoding = new Task<>() {
            @Override
            protected double[] call() {
                return geocodificacionService.geocodificar(direccionCompleta);
            }
        };

        tareaGeocoding.setOnSucceeded(event -> {
            btnCrearAlmacen.setDisable(false);
            double[] coords = tareaGeocoding.getValue();

            if (coords == null) {
                mostrarAlerta("Error", "No se pudo geocodificar la direcci\u00f3n. Verifica que sea correcta.");
                return;
            }

            almacenGlobal = new Parada(1, nombre, calle, cp, poblacion,
                    aperturaAlmacen, cierreAlmacen);
            almacenGlobal.setLatitud(coords[0]);
            almacenGlobal.setLongitud(coords[1]);

            btnCalcularRuta.setDisable(false);
            mostrarAlerta("\u00c9xito", "Almac\u00e9n creado: " + nombre);
        });

        tareaGeocoding.setOnFailed(event -> {
            btnCrearAlmacen.setDisable(false);
            mostrarAlerta("Error", "Fallo al geocodificar: " + tareaGeocoding.getException().getMessage());
        });

        Thread hilo = new Thread(tareaGeocoding);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void importarDesdePdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona el PDF de reparto");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        File archivo = fileChooser.showOpenDialog(primaryStage);
        if (archivo == null) return; // el usuario cancel\u00f3 el di\u00e1logo

        btnImportarPdf.setDisable(true);

        Task<List<FilaImportada>> tareaImportar = new Task<>() {
            @Override
            protected List<FilaImportada> call() throws Exception {
                return importadorPdfService.extraerFilas(archivo);
            }
        };

        tareaImportar.setOnSucceeded(event -> {

            List<FilaImportada> filas = tareaImportar.getValue();

            if (filas.isEmpty()) {
                btnImportarPdf.setDisable(false);
                mostrarAlerta("Aviso", "No se encontraron filas v\u00e1lidas en el PDF.");
                return;
            }

            List<FilaImportada> filasConfirmadas = VentanaPrevisualizacionPdf.mostrar(primaryStage, filas);

            if (filasConfirmadas.isEmpty()) {
                btnImportarPdf.setDisable(false);
                mostrarAlerta("Info", "Importaci\u00f3n cancelada, no se a\u00f1adi\u00f3 ninguna parada.");
                return;
            }

            geocodificarYAnadirLote(filasConfirmadas);
        });

        Thread hilo = new Thread(tareaImportar);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void geocodificarYAnadirLote(List<FilaImportada> filasConfirmadas) {

        Task<List<FilaGeocodificada>> tareaLote = new Task<>() {
            @Override
            protected List<FilaGeocodificada> call() {
                List<FilaGeocodificada> resultado = new ArrayList<>();
                int total = filasConfirmadas.size();

                for (int i = 0; i < total; i++) {
                    FilaImportada f = filasConfirmadas.get(i);
                    updateMessage("Geocodificando " + (i + 1) + " de " + total + ": " + f.getNombre());
                    updateProgress(i, total);

                    String direccionCompleta = f.getCalle() + ", " + f.getCodigoPostal() + " " + f.getPoblacion();
                    double[] coords = geocodificacionService.geocodificar(direccionCompleta);
                    resultado.add(new FilaGeocodificada(f, coords));

                    if (i < total - 1) {
                        try {
                            Thread.sleep(1100); // respeta el límite de 1 petición/seg de Nominatim
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                updateProgress(total, total);
                return resultado;
            }
        };

        Stage dialogoProgreso = crearDialogoProgreso(tareaLote);

        tareaLote.setOnSucceeded(event -> {
            dialogoProgreso.close();
            btnImportarPdf.setDisable(false);

            List<FilaGeocodificada> resultados = tareaLote.getValue();
            List<String> fallidas = new ArrayList<>();
            int anadidas = 0;

            for (FilaGeocodificada r : resultados) {
                if (r.coords == null) {
                    fallidas.add(r.fila.getNombre());
                    continue;
                }
                Parada nueva = new Parada(
                        siguienteNumeroParada++,
                        r.fila.getNombre(), r.fila.getCalle(),
                        r.fila.getCodigoPostal(), r.fila.getPoblacion(),
                        r.fila.getHoraApertura(), r.fila.getHoraCierre()
                );
                nueva.setLatitud(r.coords[0]);
                nueva.setLongitud(r.coords[1]);
                paradasIngresadas.add(nueva);
                anadidas++;
            }

            StringBuilder resumen = new StringBuilder();
            resumen.append(anadidas).append(" paradas a\u00f1adidas correctamente.");
            if (!fallidas.isEmpty()) {
                resumen.append("\n\nNo se pudieron geocodificar (a\u00f1\u00e1delas a mano si hace falta):\n");
                fallidas.forEach(n -> resumen.append("- ").append(n).append("\n"));
            }
            mostrarAlerta(fallidas.isEmpty() ? "\u00c9xito" : "Importaci\u00f3n parcial", resumen.toString());
        });

        tareaLote.setOnFailed(event -> {
            dialogoProgreso.close();
            btnImportarPdf.setDisable(false);
            mostrarAlerta("Error", "Fallo durante la importaci\u00f3n: " + tareaLote.getException().getMessage());
        });

        Thread hilo = new Thread(tareaLote);
        hilo.setDaemon(true);
        hilo.start();

        dialogoProgreso.show();
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

        String direccionCompleta = calle + ", " + cp + " " + poblacion;

        btnAnadirParada.setDisable(true);

        Task<double[]> tareaGeocoding = new Task<>() {
            @Override
            protected double[] call() {
                return geocodificacionService.geocodificar(direccionCompleta);
            }
        };

        tareaGeocoding.setOnSucceeded(event -> {
            btnAnadirParada.setDisable(false);
            double[] coords = tareaGeocoding.getValue();

            if (coords == null) {
                mostrarAlerta("Error", "No se pudo geocodificar la direcci\u00f3n. Verifica que sea correcta.");
                return;
            }

            Parada nueva = new Parada(
                    siguienteNumeroParada++,
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
        });

        tareaGeocoding.setOnFailed(event -> {
            btnAnadirParada.setDisable(false);
            mostrarAlerta("Error", "Fallo al geocodificar: " + tareaGeocoding.getException().getMessage());
        });

        Thread hilo = new Thread(tareaGeocoding);
        hilo.setDaemon(true);
        hilo.start();
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

    /**
     * JavaFX no aplica automáticamente el texto escrito a mano en un Spinner
     * editable cuando el usuario hace clic fuera de él (solo si pulsa Enter).
     * Este listener fuerza esa confirmación al perder el foco, usando el
     * converter del value factory para parsear el texto actual del editor.
     */
    private static <T> void confirmarTextoAlPerderFoco(Spinner<T> spinner) {
        spinner.focusedProperty().addListener((obs, teniaFoco, tieneFocoAhora) -> {
            if (!tieneFocoAhora) {
                SpinnerValueFactory<T> fabricaValores = spinner.getValueFactory();
                if (fabricaValores != null) {
                    try {
                        T valor = fabricaValores.getConverter().fromString(spinner.getEditor().getText());
                        fabricaValores.setValue(valor);
                    } catch (Exception ex) {
                        // Texto no parseable (p.ej. vacío o letras): se descarta y se
                        // restaura el texto del editor al último valor válido.
                        spinner.getEditor().setText(fabricaValores.getConverter().toString(fabricaValores.getValue()));
                    }
                }
            }
        });
    }

    /**
     * Resultado intermedio de geocodificar una FilaImportada: guarda la fila original
     * junto a sus coordenadas (o null si la geocodificación falló). Se usa para poder
     * construir el objeto Parada definitivo más tarde, en el hilo de JavaFX, ya que
     * el "numero" de Parada solo se puede fijar en el constructor.
     */
    private static class FilaGeocodificada {
        final FilaImportada fila;
        final double[] coords;

        FilaGeocodificada(FilaImportada fila, double[] coords) {
            this.fila = fila;
            this.coords = coords;
        }
    }

    private Stage crearDialogoProgreso(Task<?> tarea) {
        Label lbl = new Label();
        lbl.textProperty().bind(tarea.messageProperty());

        ProgressBar barra = new ProgressBar();
        barra.progressProperty().bind(tarea.progressProperty());
        barra.setPrefWidth(300);

        VBox caja = new VBox(12, lbl, barra);
        caja.setPadding(new Insets(20));
        caja.setAlignment(Pos.CENTER);

        Stage dialogo = new Stage();
        dialogo.initOwner(primaryStage);
        dialogo.initModality(Modality.WINDOW_MODAL);
        dialogo.initStyle(StageStyle.UTILITY);
        dialogo.setTitle("Importando paradas...");
        dialogo.setResizable(false);
        dialogo.setScene(new Scene(caja, 380, 110));
        return dialogo;
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