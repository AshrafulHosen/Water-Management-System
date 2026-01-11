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
    requires de.jensd.fx.glyphs.fontawesome;

    opens com.example.watermanagementsystem to javafx.fxml;
    exports com.example.watermanagementsystem;
    exports com.example.watermanagementsystem.controllers;
    exports com.example.watermanagementsystem.models;
    opens com.example.watermanagementsystem.controllers to javafx.fxml;
    opens com.example.watermanagementsystem.models to javafx.fxml;
}