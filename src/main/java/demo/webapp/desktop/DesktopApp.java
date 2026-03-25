package demo.webapp.desktop;

import demo.webapp.desktop.tabs.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main JavaFX Application class for the WorldQuant Brain Tool desktop app.
 * Replaces the web dashboard (StaticFileHandler + WebServer) with a native UI.
 *
 * Tabs:
 *   - Session  : view/update cookie, validate session
 *   - Config   : general and email settings
 *   - Filters  : regular & super alpha filter settings
 *   - Jobs     : run jobs, view job history
 *   - Logs     : real-time log viewer with filter
 *   - Results  : view progress JSON files
 */
public class DesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppState.initLogCapture();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-min-width: 90px; -fx-font-size: 13px;");

        tabPane.getTabs().addAll(
            new SessionTab().build(),
            new ConfigTab().build(),
            new FiltersTab().build(),
            new JobsTab().build(),
            new LogsTab().build(),
            new ResultsTab().build()
        );

        BorderPane root = new BorderPane(tabPane);
        root.setStyle("-fx-background-color: #f1f5f9;");

        Scene scene = new Scene(root, 1100, 780);
        primaryStage.setTitle("WorldQuant Brain Tool");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        AppState.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
