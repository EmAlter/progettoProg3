package org.progetto.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class ClientStart extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL clientUrl = getClass().getClassLoader().getResource("client-view.fxml");
        fxmlLoader.setLocation(clientUrl);
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Client");
        stage.setScene(scene);

        //Inizializzo il controller
        ClientController controller = fxmlLoader.getController();
        controller.initialize(stage);

        stage.show();


    }

    public static void main(String[] args) {
        launch(args);

    }
}

