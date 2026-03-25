package demo.webapp.desktop.tabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static demo.webapp.desktop.tabs.UiHelper.*;

public class ResultsTab {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Each progress type maps to its file name
    private record ProgressFile(String label, String fileName) {}

    private static final ProgressFile[] FILES = {
        new ProgressFile("Regular Alpha Progress",        "progress_regular.json"),
        new ProgressFile("Super Alpha Progress",          "progress_super.json"),
        new ProgressFile("Gen-Super Alpha Progress",      "progress_regular_gen_super.json")
    };

    public Tab build() {
        Tab tab = new Tab("Results");
        tab.setClosable(false);
        tab.setContent(scrollWrap(createContent()));
        return tab;
    }

    private VBox createContent() {
        VBox root = tabRoot();
        root.getChildren().add(pageTitle("Progress & Results"));

        // Refresh button row
        Button refreshBtn = primaryBtn("Refresh All");
        List<ProgressPanel> panels = new ArrayList<>();

        for (ProgressFile pf : FILES) {
            ProgressPanel panel = new ProgressPanel(pf);
            panels.add(panel);
            root.getChildren().add(panel.build());
        }

        refreshBtn.setOnAction(e -> panels.forEach(ProgressPanel::load));
        HBox topRow = new HBox(refreshBtn);
        topRow.setPadding(new Insets(0, 0, 4, 0));
        root.getChildren().add(1, topRow);   // insert after title

        // Load on creation
        panels.forEach(ProgressPanel::load);

        return root;
    }

    // ── Inner panel ──────────────────────────────────────────────────────────

    private static class ProgressPanel {

        private final ProgressFile pf;
        private Label summaryLabel;
        private TableView<AlphaRow> table;
        private final ObservableList<AlphaRow> rows = FXCollections.observableArrayList();

        ProgressPanel(ProgressFile pf) { this.pf = pf; }

        TitledPane build() {
            // Summary label
            summaryLabel = new Label("Loading...");
            summaryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_MUTED + ";");

            // Table
            table = new TableView<>(rows);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setPrefHeight(220);
            table.setStyle("-fx-font-size: 12px;");
            table.setPlaceholder(new Label("No data"));

            TableColumn<AlphaRow, String> idCol = col("Alpha ID", "alphaId");
            TableColumn<AlphaRow, String> statusCol = col("Status", "status");
            statusCol.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setGraphic(null); setText(null); }
                    else setGraphic(UiHelper.badge(s));
                }
            });
            TableColumn<AlphaRow, String> corrCol   = col("Correlation", "correlation");
            TableColumn<AlphaRow, String> submittedCol = col("Submitted", "submitted");
            TableColumn<AlphaRow, String> startedCol = col("Started At", "startedAt");
            TableColumn<AlphaRow, String> completedCol = col("Completed At", "completedAt");

            table.getColumns().addAll(idCol, statusCol, corrCol, submittedCol, startedCol, completedCol);

            VBox content = new VBox(8);
            content.setPadding(new Insets(10));
            content.getChildren().addAll(summaryLabel, table);

            TitledPane tp = new TitledPane(pf.label(), content);
            tp.setExpanded(true);
            tp.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            return tp;
        }

        void load() {
            summaryLabel.setText("Loading...");
            rows.clear();

            Task<List<AlphaRow>> task = new Task<>() {
                @Override protected List<AlphaRow> call() throws Exception {
                    Path path = Paths.get(pf.fileName());
                    if (!Files.exists(path)) return List.of();

                    JsonNode root = mapper.readTree(Files.readString(path));
                    List<AlphaRow> result = new ArrayList<>();

                    // The JSON can be either the full ProgressData object
                    // or a map of alphaId -> AlphaProgress
                    JsonNode alphasNode = root.has("alphas") ? root.get("alphas") : root;

                    Iterator<Map.Entry<String, JsonNode>> it = alphasNode.fields();
                    while (it.hasNext()) {
                        Map.Entry<String, JsonNode> entry = it.next();
                        JsonNode v = entry.getValue();
                        AlphaRow row = new AlphaRow();
                        row.alphaId    = entry.getKey();
                        row.status     = v.has("status") ? v.get("status").asText() : "—";
                        row.correlation = v.has("correlation") && !v.get("correlation").isNull()
                            ? String.format("%.4f", v.get("correlation").asDouble()) : "—";
                        row.submitted  = v.has("submitted") ? String.valueOf(v.get("submitted").asBoolean()) : "—";
                        row.startedAt  = v.has("startedAt") ? v.get("startedAt").asText() : "—";
                        row.completedAt = v.has("completedAt") && !v.get("completedAt").isNull()
                            ? v.get("completedAt").asText() : "—";
                        result.add(row);
                    }
                    return result;
                }
            };

            task.setOnSucceeded(e -> {
                List<AlphaRow> result = task.getValue();
                rows.setAll(result);
                long completed = result.stream().filter(r -> "COMPLETED".equals(r.status)).count();
                long submitted = result.stream().filter(r -> "true".equals(r.submitted)).count();
                long failed    = result.stream().filter(r -> "FAILED".equals(r.status)).count();
                summaryLabel.setText(String.format(
                    "Total: %d  |  Completed: %d  |  Submitted: %d  |  Failed: %d",
                    result.size(), completed, submitted, failed
                ));
            });

            task.setOnFailed(e -> summaryLabel.setText("Error: " + task.getException().getMessage()));
            new Thread(task, "results-load").start();
        }

        private TableColumn<AlphaRow, String> col(String header, String field) {
            TableColumn<AlphaRow, String> col = new TableColumn<>(header);
            col.setCellValueFactory(cd -> switch (field) {
                case "alphaId"     -> new SimpleStringProperty(cd.getValue().alphaId);
                case "status"      -> new SimpleStringProperty(cd.getValue().status);
                case "correlation" -> new SimpleStringProperty(cd.getValue().correlation);
                case "submitted"   -> new SimpleStringProperty(cd.getValue().submitted);
                case "startedAt"   -> new SimpleStringProperty(cd.getValue().startedAt);
                case "completedAt" -> new SimpleStringProperty(cd.getValue().completedAt);
                default            -> new SimpleStringProperty("");
            });
            return col;
        }
    }

    // ── Row model ────────────────────────────────────────────────────────────

    public static class AlphaRow {
        public String alphaId    = "";
        public String status     = "";
        public String correlation = "";
        public String submitted  = "";
        public String startedAt  = "";
        public String completedAt = "";
    }
}
