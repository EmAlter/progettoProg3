package org.progetto.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;


public class ServerStart extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        URL serverUrl = getClass().getClassLoader().getResource("server-view.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(serverUrl);
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Server");
        stage.setScene(scene);

        ServerController controller = fxmlLoader.getController();
        controller.initialize(stage);

        stage.show();


    }

    public static void main(String[] args) {
        launch();
    }
}

