package org.progetto.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;

public class ServerController {

    private ServerModel server;
    @FXML
    private ListView<String> logList;

    @FXML
    public void initialize(Stage stage) {
        try {
            server = new ServerModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(server).start();
        logList.itemsProperty().bind(server.getLogList());
        closeConnectionThroughX(stage);

    }

    //Quando chiudo la finestra con la 'X'
    public void closeConnectionThroughX(Stage stage) {
        stage.setOnCloseRequest(event -> {
            try {
                server.closeServer();
                server.deleteAllFileContent();
                Platform.exit();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
