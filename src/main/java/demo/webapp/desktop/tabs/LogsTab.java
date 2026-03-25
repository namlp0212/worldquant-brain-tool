package demo.webapp.desktop.tabs;

import demo.webapp.desktop.AppState;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

import static demo.webapp.desktop.tabs.UiHelper.*;

public class LogsTab {

    private TextArea logArea;
    private String currentFilter = "all";
    private boolean autoScroll = true;
    private Consumer<String> logListener;

    public Tab build() {
        Tab tab = new Tab("Logs");
        tab.setClosable(false);

        VBox content = createContent();

        // Register listener — appends new lines in real time
        logListener = line -> {
            if (matchesFilter(line)) {
                Platform.runLater(() -> {
                    logArea.appendText(line + "\n");
                    if (autoScroll) logArea.setScrollTop(Double.MAX_VALUE);
                });
            }
        };
        AppState.addLogListener(logListener);

        // Populate existing buffer on tab creation
        Platform.runLater(this::reloadBuffer);

        // Remove listener when tab is replaced / app closed
        tab.setOnClosed(e -> AppState.removeLogListener(logListener));

        tab.setContent(content);
        return tab;
    }

    private VBox createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");
        VBox.setVgrow(root, Priority.ALWAYS);

        // Toolbar
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label filterLbl = new Label("Filter:");
        filterLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_MUTED + ";");

        ToggleGroup tg = new ToggleGroup();
        ToggleButton allBtn     = filterBtn("All",     "all",     tg);
        ToggleButton regularBtn = filterBtn("Regular", "regular", tg);
        ToggleButton superBtn   = filterBtn("Super",   "super",   tg);
        ToggleButton errorBtn   = filterBtn("Error",   "error",   tg);
        allBtn.setSelected(true);

        tg.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                currentFilter = (String) newT.getUserData();
                reloadBuffer();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox autoScrollCb = new CheckBox("Auto-scroll");
        autoScrollCb.setSelected(true);
        autoScrollCb.setStyle("-fx-font-size: 13px;");
        autoScrollCb.selectedProperty().addListener((obs, o, n) -> autoScroll = n);

        Button clearBtn = dangerBtn("Clear");
        clearBtn.setOnAction(e -> logArea.clear());

        toolbar.getChildren().addAll(filterLbl, allBtn, regularBtn, superBtn, errorBtn,
                spacer, autoScrollCb, clearBtn);

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setStyle("-fx-font-family: 'JetBrains Mono', 'Menlo', 'Consolas', monospace;"
                + "-fx-font-size: 12px; -fx-background-color: #0f172a; -fx-text-fill: #e2e8f0;"
                + "-fx-border-color: " + COLOR_BORDER + "; -fx-border-radius: 6;"
                + "-fx-background-radius: 6; -fx-control-inner-background: #0f172a;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        root.getChildren().addAll(toolbar, logArea);
        return root;
    }

    private ToggleButton filterBtn(String label, String filter, ToggleGroup tg) {
        ToggleButton btn = new ToggleButton(label);
        btn.setToggleGroup(tg);
        btn.setUserData(filter);
        String base = "-fx-background-radius: 5; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 13px;";
        btn.setStyle(base + "-fx-background-color: #e2e8f0; -fx-text-fill: " + COLOR_TEXT + ";");
        btn.selectedProperty().addListener((obs, o, selected) -> btn.setStyle(base + (selected
            ? "-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white;"
            : "-fx-background-color: #e2e8f0; -fx-text-fill: " + COLOR_TEXT + ";")));
        return btn;
    }

    private void reloadBuffer() {
        logArea.clear();
        StringBuilder sb = new StringBuilder();
        for (String line : AppState.allLogLines) {
            if (matchesFilter(line)) sb.append(line).append("\n");
        }
        logArea.setText(sb.toString());
        if (autoScroll) logArea.setScrollTop(Double.MAX_VALUE);
    }

    private boolean matchesFilter(String line) {
        return switch (currentFilter) {
            case "regular" -> line.contains("Regular") || line.contains("regular") ||
                              (line.contains("alpha") && !line.contains("Super") && !line.contains("super"));
            case "super"   -> line.contains("Super") || line.contains("super");
            case "error"   -> line.contains("[ERROR]") || line.contains("Exception") ||
                              line.contains("FAIL") || line.contains("ERROR");
            default        -> true;
        };
    }
}
