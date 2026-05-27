module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.example to javafx.fxml;
    opens com.example.controllers to javafx.fxml;

    exports com.example;
    exports com.example.controllers;
    exports com.example.services;
    exports com.example.utils;

    exports ZONES;
    exports Sensors;
    exports Entities;
    exports Animals;
    exports Alerts;
    exports Reports;
    exports Farm;
    exports Additional_classes;
    exports Menu;
}
