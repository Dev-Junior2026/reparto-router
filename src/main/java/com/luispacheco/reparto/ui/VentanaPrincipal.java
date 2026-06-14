package com.luispacheco.reparto.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class VentanaPrincipal extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label etiqueta = new Label("Reparto Router - Fase 1");
        StackPane raiz = new StackPane(etiqueta);
        Scene escena = new Scene(raiz, 400, 300);

        primaryStage.setTitle("Reparto Router");
        primaryStage.setScene(escena);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
