module com.app {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.web;

    requires com.google.gson;

    exports com.app;

    opens com.app.controller to javafx.fxml;
    opens com.app.model to com.google.gson, javafx.base;
    opens com.app.repository to com.google.gson;
}