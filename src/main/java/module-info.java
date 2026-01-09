module com.example.watermanagementsystem {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires javafx.graphics;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.example.watermanagementsystem to javafx.fxml;
    exports com.example.watermanagementsystem;
    exports com.example.watermanagementsystem.controllers;
    opens com.example.watermanagementsystem.controllers to javafx.fxml;
}