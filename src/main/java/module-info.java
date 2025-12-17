module com.app {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.web;

    exports com.app;

    opens com.app.controller to javafx.fxml;
}
