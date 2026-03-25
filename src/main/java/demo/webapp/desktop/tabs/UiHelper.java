package demo.webapp.desktop.tabs;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Shared UI helpers to keep styling consistent across all tabs.
 */
public final class UiHelper {

    private UiHelper() {}

    public static final String COLOR_PRIMARY  = "#2563eb";
    public static final String COLOR_SUCCESS  = "#16a34a";
    public static final String COLOR_DANGER   = "#dc2626";
    public static final String COLOR_BG       = "#f1f5f9";
    public static final String COLOR_CARD     = "white";
    public static final String COLOR_TEXT     = "#1e293b";
    public static final String COLOR_MUTED    = "#64748b";
    public static final String COLOR_BORDER   = "#e2e8f0";

    /** White card with subtle shadow. */
    public static VBox card(String title) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8;"
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);");
        if (title != null) {
            Label lbl = new Label(title);
            lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");
            card.getChildren().addAll(lbl, new Separator());
        }
        return card;
    }

    /** Page title label. */
    public static Label pageTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");
        return lbl;
    }

    /** Section header inside a card. */
    public static Label sectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_MUTED + ";");
        return lbl;
    }

    /** Standard primary button (blue). */
    public static Button primaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white;"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white;"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white;"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"));
        return btn;
    }

    /** Standard secondary button (gray). */
    public static Button secondaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: " + COLOR_TEXT + ";"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: " + COLOR_TEXT + ";"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: " + COLOR_TEXT + ";"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;"));
        return btn;
    }

    /** Success (green) button. */
    public static Button successBtn(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color: " + COLOR_SUCCESS + "; -fx-text-fill: white;"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(COLOR_SUCCESS, "#15803d")));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    /** Danger (red) button. */
    public static Button dangerBtn(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color: " + COLOR_DANGER + "; -fx-text-fill: white;"
                + "-fx-background-radius: 6; -fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 13px;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(COLOR_DANGER, "#b91c1c")));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    /** Standard text field with border. */
    public static TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: white; -fx-border-color: " + COLOR_BORDER
                + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 6 10;");
        return tf;
    }

    /** ComboBox with border. */
    public static <T> ComboBox<T> combo() {
        ComboBox<T> cb = new ComboBox<>();
        cb.setStyle("-fx-background-color: white; -fx-border-color: " + COLOR_BORDER
                + "; -fx-border-radius: 5; -fx-background-radius: 5;");
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    /**
     * Create a standard form GridPane (label + field pairs).
     * Use addFormRow() to populate it.
     */
    public static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(160);
        ColumnConstraints c2 = new ColumnConstraints(200, 300, Double.MAX_VALUE);
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        return grid;
    }

    /** Add a label + control pair to a form grid at the given row. */
    public static void formRow(GridPane grid, String labelText, javafx.scene.Node control, int row) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_MUTED + ";");
        grid.add(lbl, 0, row);
        grid.add(control, 1, row);
    }

    /** A message label (success / error). Call setMsg() to update it. */
    public static Label msgLabel() {
        Label lbl = new Label();
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 13px;");
        return lbl;
    }

    /** Set message label text with color: true=green, false=red, null=gray. */
    public static void setMsg(Label lbl, String text, Boolean success) {
        lbl.setText(text);
        String color = success == null ? COLOR_MUTED : (success ? COLOR_SUCCESS : COLOR_DANGER);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
    }

    /** Root VBox for a tab's scroll content. */
    public static VBox tabRoot() {
        VBox vb = new VBox(20);
        vb.setPadding(new Insets(24));
        vb.setStyle("-fx-background-color: " + COLOR_BG + ";");
        return vb;
    }

    /** Wrap a node in a ScrollPane that fills the tab. */
    public static ScrollPane scrollWrap(javafx.scene.Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: " + COLOR_BG + "; -fx-background: " + COLOR_BG + ";");
        return sp;
    }

    /** Status badge label. */
    public static Label badge(String text) {
        Label lbl = new Label(text);
        String color = switch (text) {
            case "RUNNING"   -> "#d97706";
            case "COMPLETED" -> COLOR_SUCCESS;
            case "FAILED"    -> COLOR_DANGER;
            default          -> COLOR_MUTED;
        };
        lbl.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";"
                + "-fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 12px; -fx-font-weight: bold;");
        return lbl;
    }
}
