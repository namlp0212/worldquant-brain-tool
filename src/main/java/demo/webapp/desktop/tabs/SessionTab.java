package demo.webapp.desktop.tabs;

import demo.webapp.ConfigLoader;
import demo.webapp.Constant;
import demo.webapp.SessionValidator;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;

import static demo.webapp.desktop.tabs.UiHelper.*;

public class SessionTab {

    private Circle statusDot;
    private Label statusLabel;
    private Label usernameLabel, emailLabel, userIdLabel, cookiePreviewLabel;
    private TextArea cookieInput;
    private Label msgLabel;
    private Button updateBtn;

    public Tab build() {
        Tab tab = new Tab("Session");
        tab.setClosable(false);
        tab.setContent(scrollWrap(createContent()));
        return tab;
    }

    private VBox createContent() {
        VBox root = tabRoot();
        root.getChildren().addAll(
            pageTitle("Session"),
            buildStatusCard(),
            buildUpdateCard()
        );
        checkSession();
        return root;
    }

    // ── Status card ─────────────────────────────────────────────────────────

    private VBox buildStatusCard() {
        VBox card = card("Session Info");

        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusDot = new Circle(8, Color.GRAY);
        statusLabel = new Label("Checking...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_MUTED + ";");
        statusRow.getChildren().addAll(statusDot, statusLabel);

        GridPane grid = formGrid();
        grid.setPadding(new Insets(8, 0, 0, 0));

        usernameLabel     = new Label("-");
        emailLabel        = new Label("-");
        userIdLabel       = new Label("-");
        cookiePreviewLabel = new Label("-");
        cookiePreviewLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: " + COLOR_MUTED + ";");

        for (Label v : new Label[]{usernameLabel, emailLabel, userIdLabel}) {
            v.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT + ";");
        }

        formRow(grid, "Username:", usernameLabel, 0);
        formRow(grid, "Email:", emailLabel, 1);
        formRow(grid, "User ID:", userIdLabel, 2);
        formRow(grid, "Cookie Preview:", cookiePreviewLabel, 3);

        Button checkBtn = secondaryBtn("Refresh Status");
        checkBtn.setOnAction(e -> checkSession());

        card.getChildren().addAll(statusRow, grid, checkBtn);
        return card;
    }

    // ── Update cookie card ──────────────────────────────────────────────────

    private VBox buildUpdateCard() {
        VBox card = card("Update Cookie");

        cookieInput = new TextArea();
        cookieInput.setPromptText("Paste new cookie value here...");
        cookieInput.setPrefRowCount(4);
        cookieInput.setWrapText(true);
        cookieInput.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-border-color: "
                + COLOR_BORDER + "; -fx-border-radius: 5; -fx-background-radius: 5;");

        msgLabel = msgLabel();
        updateBtn = primaryBtn("Update & Validate Cookie");
        updateBtn.setOnAction(e -> updateCookie());

        card.getChildren().addAll(cookieInput, updateBtn, msgLabel);
        return card;
    }

    // ── Logic ────────────────────────────────────────────────────────────────

    private void checkSession() {
        statusLabel.setText("Checking...");
        statusDot.setFill(Color.GRAY);

        Task<SessionValidator.ValidationResult> task = new Task<>() {
            @Override protected SessionValidator.ValidationResult call() {
                return SessionValidator.validate();
            }
        };
        task.setOnSucceeded(e -> applyResult(task.getValue()));
        task.setOnFailed(e -> {
            statusDot.setFill(Color.RED);
            statusLabel.setText("Error: " + task.getException().getMessage());
        });
        new Thread(task, "session-check").start();
    }

    private void applyResult(SessionValidator.ValidationResult r) {
        if (r.isValid()) {
            statusDot.setFill(Color.web(COLOR_SUCCESS));
            statusLabel.setText("Valid");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_SUCCESS + ";");
            usernameLabel.setText(r.getUsername());
            emailLabel.setText(r.getEmail());
            userIdLabel.setText(r.getUserId());
        } else {
            statusDot.setFill(Color.web(COLOR_DANGER));
            statusLabel.setText("Invalid — " + r.getMessage());
            statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_DANGER + ";");
            usernameLabel.setText("-");
            emailLabel.setText("-");
            userIdLabel.setText("-");
        }
        String cookie = ConfigLoader.getCookie();
        if (cookie != null && cookie.length() > 50) {
            cookiePreviewLabel.setText(cookie.substring(0, 30) + "..." + cookie.substring(cookie.length() - 20));
        } else {
            cookiePreviewLabel.setText(cookie != null && !cookie.isBlank() ? cookie : "Not set");
        }
    }

    private void updateCookie() {
        String newCookie = cookieInput.getText().trim();
        if (newCookie.isEmpty()) {
            setMsg(msgLabel, "Please paste a cookie first.", false);
            return;
        }
        updateBtn.setDisable(true);
        setMsg(msgLabel, "Updating...", null);

        Task<SessionValidator.ValidationResult> task = new Task<>() {
            @Override protected SessionValidator.ValidationResult call() throws Exception {
                Path path = Paths.get("config.properties");
                Properties props = new Properties();
                if (Files.exists(path)) {
                    try (InputStream is = Files.newInputStream(path)) { props.load(is); }
                }
                props.setProperty("wq.cookie", newCookie);
                try (OutputStream os = Files.newOutputStream(path)) {
                    props.store(os, "Cookie updated at " + Instant.now());
                }
                ConfigLoader.reload();
                Constant.refreshCookie();
                return SessionValidator.validate();
            }
        };

        task.setOnSucceeded(e -> {
            SessionValidator.ValidationResult r = task.getValue();
            applyResult(r);
            if (r.isValid()) {
                setMsg(msgLabel, "Cookie updated! Session valid. Welcome, " + r.getUsername(), true);
                cookieInput.clear();
            } else {
                setMsg(msgLabel, "Cookie saved but session invalid: " + r.getMessage(), false);
            }
            updateBtn.setDisable(false);
        });
        task.setOnFailed(e -> {
            setMsg(msgLabel, "Error: " + task.getException().getMessage(), false);
            updateBtn.setDisable(false);
        });
        new Thread(task, "cookie-update").start();
    }
}
