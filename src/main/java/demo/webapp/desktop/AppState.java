package demo.webapp.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Shared application state for the desktop app.
 * Holds the log buffer, running jobs list, and the background executor.
 * Intercepts System.out/err to capture all log output.
 */
public class AppState {

    public static final int MAX_LOG_LINES = 5000;
    public static final ObservableList<String> allLogLines = FXCollections.observableArrayList();
    public static final ObservableList<JobModel> jobs = FXCollections.observableArrayList();
    public static final ExecutorService jobExecutor = Executors.newFixedThreadPool(3);

    private static final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private static boolean logCaptureInitialized = false;

    // ── Job model ────────────────────────────────────────────────────────────

    public static class JobModel {
        public final String id;
        public final String type;
        public volatile String status;
        public final String startedAt;
        public volatile String completedAt;
        public volatile String error;

        public JobModel(String type) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.type = type;
            this.status = "RUNNING";
            this.startedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    // ── Log listeners ────────────────────────────────────────────────────────

    public static void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    public static void removeLogListener(Consumer<String> listener) {
        logListeners.remove(listener);
    }

    // ── Log capture ──────────────────────────────────────────────────────────

    public static void initLogCapture() {
        if (logCaptureInitialized) return;
        logCaptureInitialized = true;

        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(originalOut) {
            @Override public void println(String x)  { super.println(x); if (x != null) addLog(x, false); }
            @Override public void println(Object x)  { super.println(x); if (x != null) addLog(String.valueOf(x), false); }
        });

        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(originalErr) {
            @Override public void println(String x)  { super.println(x); if (x != null) addLog(x, true); }
            @Override public void println(Object x)  { super.println(x); if (x != null) addLog(String.valueOf(x), true); }
        });
    }

    public static void addLog(String line, boolean isError) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logLine = "[" + ts + "] " + (isError ? "[ERROR] " : "") + line;

        for (Consumer<String> listener : logListeners) {
            try { listener.accept(logLine); } catch (Exception ignored) {}
        }

        Platform.runLater(() -> {
            allLogLines.add(logLine);
            while (allLogLines.size() > MAX_LOG_LINES) allLogLines.remove(0);
        });
    }

    // ── Job runner ───────────────────────────────────────────────────────────

    public static JobModel runJob(String type, boolean clearProgress, Runnable onComplete) {
        JobModel job = new JobModel(type);
        Platform.runLater(() -> jobs.add(0, job));

        String[] args = clearProgress ? new String[]{"--clear"} : new String[]{};

        jobExecutor.submit(() -> {
            try {
                switch (type.toLowerCase()) {
                    case "regular"      -> demo.webapp.regular.RegularAlphaUtils.main(args);
                    case "super"        -> demo.webapp.regular.SuperAlphaUtils.main(args);
                    case "gen-super"    -> demo.webapp.regular.RegularAlphaForGenSuperAlphaUtils.main(args);
                    case "mark-failed"  -> demo.webapp.regular.MarkFailedAlphasUtils.main(args);
                    default -> throw new IllegalArgumentException("Unknown job type: " + type);
                }
                job.status = "COMPLETED";
            } catch (Exception e) {
                job.status = "FAILED";
                job.error = e.getMessage();
            } finally {
                job.completedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                if (onComplete != null) Platform.runLater(onComplete);
            }
        });

        return job;
    }

    // ── Shutdown ─────────────────────────────────────────────────────────────

    public static void shutdown() {
        jobExecutor.shutdown();
        try {
            jobExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
