module com.test_2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires org.xerial.sqlitejdbc;
    requires jbcrypt;

    opens com.test_2 to javafx.fxml;
    opens com.test_2.controllers to javafx.fxml;
    opens com.test_2.models to javafx.fxml;
    opens com.test_2.database to javafx.fxml;
    
    exports com.test_2;
    exports com.test_2.controllers;
    exports com.test_2.models;
    exports com.test_2.database;
}