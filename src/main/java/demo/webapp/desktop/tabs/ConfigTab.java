package demo.webapp.desktop.tabs;

import demo.webapp.ConfigLoader;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static demo.webapp.desktop.tabs.UiHelper.*;

public class ConfigTab {

    public Tab build() {
        Tab tab = new Tab("Config");
        tab.setClosable(false);
        tab.setContent(scrollWrap(createContent()));
        return tab;
    }

    private VBox createContent() {
        VBox root = tabRoot();
        root.getChildren().addAll(
            pageTitle("Configuration"),
            buildGeneralCard(),
            buildEmailCard()
        );
        return root;
    }

    // ── General settings ────────────────────────────────────────────────────

    private VBox buildGeneralCard() {
        VBox card = card("General Settings");

        TextField threadPoolField = field("");
        threadPoolField.setText(String.valueOf(ConfigLoader.getThreadPoolSize()));
        TextField minCorrField = field("");
        minCorrField.setText(String.valueOf(ConfigLoader.getMinCorrelation()));
        TextField minFitnessField = field("");
        minFitnessField.setText(String.valueOf(ConfigLoader.getMinFitness()));

        GridPane grid = formGrid();
        formRow(grid, "Thread Pool Size:", threadPoolField, 0);
        formRow(grid, "Min Correlation:", minCorrField, 1);
        formRow(grid, "Min Fitness:", minFitnessField, 2);

        Label msg = msgLabel();
        Button saveBtn = primaryBtn("Save General Settings");
        saveBtn.setOnAction(e -> save(new String[][]{
            {"thread.pool.size",       threadPoolField.getText()},
            {"alpha.min.correlation",  minCorrField.getText()},
            {"alpha.min.fitness",      minFitnessField.getText()}
        }, msg, saveBtn));

        card.getChildren().addAll(grid, saveBtn, msg);
        return card;
    }

    // ── Email settings ──────────────────────────────────────────────────────

    private VBox buildEmailCard() {
        VBox card = card("Email / SMTP Settings");

        TextField smtpHostField = field("");
        smtpHostField.setText(ConfigLoader.getSmtpHost());
        TextField smtpPortField = field("");
        smtpPortField.setText(String.valueOf(ConfigLoader.getSmtpPort()));
        TextField smtpUserField = field("");
        smtpUserField.setText(ConfigLoader.getSmtpUsername());
        PasswordField smtpPassField = new PasswordField();
        smtpPassField.setText(ConfigLoader.getSmtpPassword());
        smtpPassField.setStyle("-fx-background-color: white; -fx-border-color: " + COLOR_BORDER
                + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 6 10;");
        TextField recipientField = field("");
        recipientField.setText(ConfigLoader.getEmailRecipient());

        GridPane grid = formGrid();
        formRow(grid, "SMTP Host:", smtpHostField, 0);
        formRow(grid, "SMTP Port:", smtpPortField, 1);
        formRow(grid, "Username:", smtpUserField, 2);
        formRow(grid, "Password:", smtpPassField, 3);
        formRow(grid, "Recipient:", recipientField, 4);

        Label msg = msgLabel();
        Button saveBtn = primaryBtn("Save Email Settings");
        saveBtn.setOnAction(e -> {
            boolean hasPass = !smtpPassField.getText().isBlank();
            String[][] pairs = hasPass ? new String[][]{
                {"smtp.host",        smtpHostField.getText()},
                {"smtp.port",        smtpPortField.getText()},
                {"smtp.username",    smtpUserField.getText()},
                {"smtp.password",    smtpPassField.getText()},
                {"email.recipient",  recipientField.getText()}
            } : new String[][]{
                {"smtp.host",        smtpHostField.getText()},
                {"smtp.port",        smtpPortField.getText()},
                {"smtp.username",    smtpUserField.getText()},
                {"email.recipient",  recipientField.getText()}
            };
            save(pairs, msg, saveBtn);
        });

        card.getChildren().addAll(grid, saveBtn, msg);
        return card;
    }

    // ── Save helper ──────────────────────────────────────────────────────────

    private void save(String[][] keyValues, Label msg, Button btn) {
        btn.setDisable(true);
        setMsg(msg, "Saving...", null);

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                Path configPath = Paths.get("config.properties");
                Properties props = new Properties();
                if (Files.exists(configPath)) {
                    try (InputStream is = Files.newInputStream(configPath)) { props.load(is); }
                }
                for (String[] kv : keyValues) {
                    if (kv[1] != null && !kv[1].isBlank()) props.setProperty(kv[0], kv[1]);
                }
                try (OutputStream os = Files.newOutputStream(configPath)) {
                    props.store(os, "Updated via Desktop App");
                }
                ConfigLoader.reload();
                return null;
            }
        };
        task.setOnSucceeded(e -> { setMsg(msg, "Saved successfully!", true); btn.setDisable(false); });
        task.setOnFailed(e -> { setMsg(msg, "Error: " + task.getException().getMessage(), false); btn.setDisable(false); });
        new Thread(task, "config-save").start();
    }
}
