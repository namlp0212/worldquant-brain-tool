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

public class FiltersTab {

    private static final String[] REGIONS = {"JPN", "USA", "EUR", "ASI", "IND", "CHN", "KOR", "TWN", "GLB"};
    private static final String[] STATUSES = {
        "UNSUBMITTED%1FIS_FAIL",
        "UNSUBMITTED",
        "SUBMITTED",
        "IS_FAIL"
    };

    public Tab build() {
        Tab tab = new Tab("Filters");
        tab.setClosable(false);
        tab.setContent(scrollWrap(createContent()));
        return tab;
    }

    private VBox createContent() {
        VBox root = tabRoot();
        root.getChildren().addAll(
            pageTitle("Alpha Filter Settings"),
            buildRegularCard(),
            buildSuperCard()
        );
        return root;
    }

    // ── Regular filters ─────────────────────────────────────────────────────

    private VBox buildRegularCard() {
        VBox card = card("Regular Alpha Filters");

        ComboBox<String> regionCb = combo();
        regionCb.getItems().addAll(REGIONS);
        regionCb.setValue(ConfigLoader.getRegularFilterRegion());

        TextField dateFromField = field("e.g. 2026-01-01T00:00:00-05:00");
        dateFromField.setText(ConfigLoader.getRegularFilterDateFrom());
        TextField dateToField = field("e.g. 2026-02-01T00:00:00-05:00");
        dateToField.setText(ConfigLoader.getRegularFilterDateTo());

        TextField minFitnessField = field("e.g. 1.0");
        minFitnessField.setText(String.valueOf(ConfigLoader.getRegularFilterMinFitness()));

        TextField limitField = field("e.g. 5");
        limitField.setText(String.valueOf(ConfigLoader.getRegularFilterLimit()));

        ComboBox<String> statusCb = combo();
        statusCb.getItems().addAll(STATUSES);
        statusCb.setValue(ConfigLoader.getRegularFilterStatus());

        CheckBox favoriteCb = new CheckBox("Favorite only");
        favoriteCb.setSelected(ConfigLoader.getRegularFilterFavorite());
        favoriteCb.setStyle("-fx-font-size: 13px;");

        GridPane grid = formGrid();
        formRow(grid, "Region:", regionCb, 0);
        formRow(grid, "Date From:", dateFromField, 1);
        formRow(grid, "Date To:", dateToField, 2);
        formRow(grid, "Min Fitness:", minFitnessField, 3);
        formRow(grid, "Limit:", limitField, 4);
        formRow(grid, "Status:", statusCb, 5);
        formRow(grid, "Favorite:", favoriteCb, 6);

        Label msg = msgLabel();
        Button saveBtn = primaryBtn("Save Regular Filters");
        saveBtn.setOnAction(e -> save(new String[][]{
            {"filter.regular.region",       regionCb.getValue()},
            {"filter.regular.date.from",    dateFromField.getText()},
            {"filter.regular.date.to",      dateToField.getText()},
            {"filter.regular.min.fitness",  minFitnessField.getText()},
            {"filter.regular.limit",        limitField.getText()},
            {"filter.regular.status",       statusCb.getValue()},
            {"filter.regular.favorite",     String.valueOf(favoriteCb.isSelected())}
        }, msg, saveBtn));

        card.getChildren().addAll(grid, saveBtn, msg);
        return card;
    }

    // ── Super filters ───────────────────────────────────────────────────────

    private VBox buildSuperCard() {
        VBox card = card("Super Alpha Filters");

        ComboBox<String> regionCb = combo();
        regionCb.getItems().addAll(REGIONS);
        regionCb.setValue(ConfigLoader.getSuperFilterRegion());

        TextField dateFromField = field("e.g. 2026-01-01T00:00:00-05:00");
        dateFromField.setText(ConfigLoader.getSuperFilterDateFrom());
        TextField dateToField = field("e.g. 2026-02-01T00:00:00-05:00");
        dateToField.setText(ConfigLoader.getSuperFilterDateTo());

        TextField limitField = field("e.g. 10");
        limitField.setText(String.valueOf(ConfigLoader.getSuperFilterLimit()));

        ComboBox<String> statusCb = combo();
        statusCb.getItems().addAll(STATUSES);
        statusCb.setValue(ConfigLoader.getSuperFilterStatus());

        CheckBox favoriteCb = new CheckBox("Favorite only");
        favoriteCb.setSelected(ConfigLoader.getSuperFilterFavorite());
        favoriteCb.setStyle("-fx-font-size: 13px;");

        GridPane grid = formGrid();
        formRow(grid, "Region:", regionCb, 0);
        formRow(grid, "Date From:", dateFromField, 1);
        formRow(grid, "Date To:", dateToField, 2);
        formRow(grid, "Limit:", limitField, 3);
        formRow(grid, "Status:", statusCb, 4);
        formRow(grid, "Favorite:", favoriteCb, 5);

        Label msg = msgLabel();
        Button saveBtn = primaryBtn("Save Super Filters");
        saveBtn.setOnAction(e -> save(new String[][]{
            {"filter.super.region",    regionCb.getValue()},
            {"filter.super.date.from", dateFromField.getText()},
            {"filter.super.date.to",   dateToField.getText()},
            {"filter.super.limit",     limitField.getText()},
            {"filter.super.status",    statusCb.getValue()},
            {"filter.super.favorite",  String.valueOf(favoriteCb.isSelected())}
        }, msg, saveBtn));

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
                    if (kv[1] != null) props.setProperty(kv[0], kv[1]);
                }
                try (OutputStream os = Files.newOutputStream(configPath)) {
                    props.store(os, "Filters updated via Desktop App");
                }
                ConfigLoader.reload();
                return null;
            }
        };
        task.setOnSucceeded(e -> { setMsg(msg, "Saved successfully!", true); btn.setDisable(false); });
        task.setOnFailed(e -> { setMsg(msg, "Error: " + task.getException().getMessage(), false); btn.setDisable(false); });
        new Thread(task, "filter-save").start();
    }
}
