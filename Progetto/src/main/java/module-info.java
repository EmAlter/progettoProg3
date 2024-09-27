module org.progetto {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.progetto.server to javafx.fxml;
    exports org.progetto.server;

    opens org.progetto.client to javafx.fxml;
    exports org.progetto.client;
    exports org.progetto.email;
    opens org.progetto.email to javafx.fxml;

}