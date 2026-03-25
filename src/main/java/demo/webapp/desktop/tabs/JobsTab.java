package demo.webapp.desktop.tabs;

import demo.webapp.SessionValidator;
import demo.webapp.desktop.AppState;
import demo.webapp.desktop.AppState.JobModel;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import static demo.webapp.desktop.tabs.UiHelper.*;

public class JobsTab {

    private Label sessionStatusLabel;

    public Tab build() {
        Tab tab = new Tab("Jobs");
        tab.setClosable(false);
        tab.setContent(scrollWrap(createContent()));
        return tab;
    }

    private VBox createContent() {
        VBox root = tabRoot();
        root.getChildren().addAll(
            pageTitle("Job Control"),
            buildRunCard(),
            buildMarkFailedCard(),
            buildHistoryCard()
        );
        return root;
    }

    // ── Run card ────────────────────────────────────────────────────────────

    private VBox buildRunCard() {
        VBox card = card("Run Jobs");

        // Session status row
        HBox sessionRow = new HBox(8);
        sessionRow.setAlignment(Pos.CENTER_LEFT);
        Label sessionLbl = new Label("Session:");
        sessionLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_MUTED + ";");
        sessionStatusLabel = new Label("—");
        sessionStatusLabel.setStyle("-fx-font-size: 13px;");
        sessionRow.getChildren().addAll(sessionLbl, sessionStatusLabel);

        // Clear progress checkbox
        CheckBox clearCb = new CheckBox("Clear progress before run");
        clearCb.setStyle("-fx-font-size: 13px;");

        // Run buttons
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Button runRegularBtn  = successBtn("▶  Run Regular");
        Button runSuperBtn    = primaryBtn("▶  Run Super");
        Button runGenSuperBtn = secondaryBtn("▶  Run Gen-Super");

        Label msgLabel = msgLabel();

        runRegularBtn.setOnAction(e  -> runJob("regular",   clearCb.isSelected(), msgLabel));
        runSuperBtn.setOnAction(e    -> runJob("super",     clearCb.isSelected(), msgLabel));
        runGenSuperBtn.setOnAction(e -> runJob("gen-super", clearCb.isSelected(), msgLabel));

        Button checkSessionBtn = secondaryBtn("Check Session");
        checkSessionBtn.setOnAction(e -> refreshSessionStatus(checkSessionBtn));
        sessionRow.getChildren().add(checkSessionBtn);

        btnRow.getChildren().addAll(runRegularBtn, runSuperBtn, runGenSuperBtn);
        card.getChildren().addAll(sessionRow, clearCb, btnRow, msgLabel);

        return card;
    }

    // ── Mark Failed card ────────────────────────────────────────────────────

    private VBox buildMarkFailedCard() {
        VBox card = card("Mark Not-Pass Alphas as Favorite");

        Label desc = new Label(
            "Loops up to 1000 alphas from the saved Regular filter.\n" +
            "Any alpha with at least one FAIL check will be marked as Favorite."
        );
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_MUTED + ";");

        Button runBtn = primaryBtn("Mark Failed Alphas as Favorite");
        Label msgLabel = msgLabel();

        runBtn.setOnAction(e -> {
            SessionValidator.ValidationResult r = SessionValidator.validate();
            if (!r.isValid()) {
                setMsg(msgLabel, "Session invalid: " + r.getMessage() + " — update your cookie first.", false);
                return;
            }
            setMsg(msgLabel, "Job 'mark-failed' started.", true);
            runBtn.setDisable(true);
            AppState.runJob("mark-failed", false, () -> {
                setMsg(msgLabel, "Job 'mark-failed' finished.", true);
                runBtn.setDisable(false);
            });
        });

        card.getChildren().addAll(desc, runBtn, msgLabel);
        return card;
    }

    // ── History table ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private VBox buildHistoryCard() {
        VBox card = card("Job History");

        TableView<JobModel> table = new TableView<>(AppState.jobs);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(350);
        table.setStyle("-fx-font-size: 13px;");
        table.setPlaceholder(new Label("No jobs yet. Click a Run button to start."));

        TableColumn<JobModel, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().id));
        idCol.setMaxWidth(80);

        TableColumn<JobModel, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().type.toUpperCase()));

        TableColumn<JobModel, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().status));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else setGraphic(badge(item));
            }
        });

        TableColumn<JobModel, String> startedCol = new TableColumn<>("Started");
        startedCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().startedAt));

        TableColumn<JobModel, String> completedCol = new TableColumn<>("Completed");
        completedCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().completedAt != null ? cd.getValue().completedAt : "—"
        ));

        TableColumn<JobModel, String> errorCol = new TableColumn<>("Error");
        errorCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().error != null ? cd.getValue().error : ""
        ));

        table.getColumns().addAll(idCol, typeCol, statusCol, startedCol, completedCol, errorCol);

        // Auto-refresh table every 2 seconds to reflect volatile status changes
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(2), e -> table.refresh()));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();

        Button clearBtn = dangerBtn("Clear History");
        clearBtn.setOnAction(e -> AppState.jobs.clear());

        card.getChildren().addAll(table, clearBtn);
        return card;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void runJob(String type, boolean clearProgress, Label msgLabel) {
        // Quick session check
        SessionValidator.ValidationResult r = SessionValidator.validate();
        if (!r.isValid()) {
            setMsg(msgLabel, "Session invalid: " + r.getMessage() + " — update your cookie first.", false);
            return;
        }
        setMsg(msgLabel, "Job '" + type + "' started.", true);
        AppState.runJob(type, clearProgress, () -> setMsg(msgLabel, "Job '" + type + "' finished.", true));
    }

    private void refreshSessionStatus(Button btn) {
        btn.setDisable(true);
        sessionStatusLabel.setText("Checking...");
        sessionStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_MUTED + ";");
        new Thread(() -> {
            SessionValidator.ValidationResult r = SessionValidator.validate();
            javafx.application.Platform.runLater(() -> {
                if (r.isValid()) {
                    sessionStatusLabel.setText("Valid (" + r.getUsername() + ")");
                    sessionStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SUCCESS + ";");
                } else {
                    sessionStatusLabel.setText("Invalid");
                    sessionStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_DANGER + ";");
                }
                btn.setDisable(false);
            });
        }, "session-check").start();
    }
}
