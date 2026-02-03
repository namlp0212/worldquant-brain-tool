package demo.webapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks progress of alpha processing to enable resume capability.
 * Progress is saved to a JSON file and can be resumed after crashes or interruptions.
 */
public class ProgressTracker {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final String progressFilePath;
    private final ProgressData data;
    private final Object saveLock = new Object();

    /**
     * Status of an individual alpha in the processing pipeline.
     */
    public enum AlphaStatus {
        PENDING,        // Not yet started
        NAME_SET,       // Name has been set
        CORR_CHECKED,   // Correlation has been checked
        FAVORITED,      // Marked as favorite (if applicable)
        SUBMITTED,      // Submitted to WorldQuant
        COMPLETED,      // Fully processed
        FAILED,         // Processing failed
        SKIPPED         // Skipped (e.g., low correlation)
    }

    /**
     * Data for a single alpha's progress.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlphaProgress {
        public String alphaId;
        public AlphaStatus status;
        public Double correlation;
        public String startedAt;
        public String completedAt;
        public String errorMessage;
        public boolean submitted;

        public AlphaProgress() {}

        public AlphaProgress(String alphaId) {
            this.alphaId = alphaId;
            this.status = AlphaStatus.PENDING;
            this.startedAt = Instant.now().toString();
        }
    }

    /**
     * Container for all progress data, serialized to JSON.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgressData {
        public String jobType;           // REGULAR, SUPER, etc.
        public String startedAt;
        public String lastUpdatedAt;
        public int totalAlphas;
        public int processedCount;
        public int successCount;
        public int failedCount;
        public Map<String, AlphaProgress> alphas = new ConcurrentHashMap<>();

        public ProgressData() {}

        public ProgressData(String jobType) {
            this.jobType = jobType;
            this.startedAt = Instant.now().toString();
            this.lastUpdatedAt = this.startedAt;
        }
    }

    /**
     * Creates a new ProgressTracker for the specified job type.
     *
     * @param jobType Type of job (e.g., "REGULAR", "SUPER")
     */
    public ProgressTracker(String jobType) {
        this(jobType, "progress_" + jobType.toLowerCase() + ".json");
    }

    /**
     * Creates a new ProgressTracker with a custom file path.
     *
     * @param jobType Type of job
     * @param filePath Path to the progress file
     */
    public ProgressTracker(String jobType, String filePath) {
        this.progressFilePath = filePath;
        this.data = loadOrCreate(jobType);
    }

    /**
     * Loads existing progress or creates a new progress file.
     */
    private ProgressData loadOrCreate(String jobType) {
        Path path = Paths.get(progressFilePath);

        if (Files.exists(path)) {
            try {
                ProgressData loaded = mapper.readValue(path.toFile(), ProgressData.class);
                System.out.println("Loaded existing progress from: " + progressFilePath);
                System.out.println("  Job type: " + loaded.jobType);
                System.out.println("  Total alphas: " + loaded.totalAlphas);
                System.out.println("  Processed: " + loaded.processedCount);
                System.out.println("  Success: " + loaded.successCount);
                System.out.println("  Failed: " + loaded.failedCount);

                // Ensure the map is concurrent
                if (!(loaded.alphas instanceof ConcurrentHashMap)) {
                    loaded.alphas = new ConcurrentHashMap<>(loaded.alphas);
                }

                return loaded;
            } catch (IOException e) {
                System.err.println("Failed to load progress file, starting fresh: " + e.getMessage());
            }
        }

        System.out.println("Creating new progress tracker for job type: " + jobType);
        return new ProgressData(jobType);
    }

    /**
     * Initializes tracking for a list of alpha IDs.
     * Already tracked alphas are preserved.
     *
     * @param alphaIds List of alpha IDs to track
     */
    public void initializeAlphas(List<String> alphaIds) {
        data.totalAlphas = alphaIds.size();

        for (String alphaId : alphaIds) {
            if (!data.alphas.containsKey(alphaId)) {
                data.alphas.put(alphaId, new AlphaProgress(alphaId));
            }
        }

        save();
    }

    /**
     * Checks if an alpha has already been fully processed.
     *
     * @param alphaId The alpha ID to check
     * @return true if alpha is completed, submitted, or skipped
     */
    public boolean isAlreadyProcessed(String alphaId) {
        AlphaProgress progress = data.alphas.get(alphaId);
        if (progress == null) {
            return false;
        }

        return progress.status == AlphaStatus.COMPLETED ||
               progress.status == AlphaStatus.SUBMITTED ||
               progress.status == AlphaStatus.SKIPPED;
    }

    /**
     * Checks if an alpha's name has already been set.
     */
    public boolean isNameSet(String alphaId) {
        AlphaProgress progress = data.alphas.get(alphaId);
        if (progress == null) {
            return false;
        }

        return progress.status.ordinal() >= AlphaStatus.NAME_SET.ordinal() &&
               progress.status != AlphaStatus.FAILED;
    }

    /**
     * Checks if an alpha's correlation has already been checked.
     */
    public boolean isCorrelationChecked(String alphaId) {
        AlphaProgress progress = data.alphas.get(alphaId);
        if (progress == null) {
            return false;
        }

        return progress.correlation != null &&
               progress.status.ordinal() >= AlphaStatus.CORR_CHECKED.ordinal() &&
               progress.status != AlphaStatus.FAILED;
    }

    /**
     * Checks if an alpha has already been submitted.
     */
    public boolean isSubmitted(String alphaId) {
        AlphaProgress progress = data.alphas.get(alphaId);
        return progress != null && progress.submitted;
    }

    /**
     * Gets the cached correlation value for an alpha.
     *
     * @return The correlation value, or null if not yet checked
     */
    public Double getCorrelation(String alphaId) {
        AlphaProgress progress = data.alphas.get(alphaId);
        return progress != null ? progress.correlation : null;
    }

    /**
     * Updates the status of an alpha.
     */
    public void updateStatus(String alphaId, AlphaStatus status) {
        AlphaProgress progress = data.alphas.computeIfAbsent(alphaId, AlphaProgress::new);
        progress.status = status;

        if (status == AlphaStatus.COMPLETED || status == AlphaStatus.FAILED || status == AlphaStatus.SKIPPED) {
            progress.completedAt = Instant.now().toString();
            data.processedCount++;

            if (status == AlphaStatus.COMPLETED || status == AlphaStatus.SKIPPED) {
                data.successCount++;
            } else {
                data.failedCount++;
            }
        }

        save();
    }

    /**
     * Marks that the name has been set for an alpha.
     */
    public void markNameSet(String alphaId) {
        AlphaProgress progress = data.alphas.computeIfAbsent(alphaId, AlphaProgress::new);
        if (progress.status.ordinal() < AlphaStatus.NAME_SET.ordinal()) {
            progress.status = AlphaStatus.NAME_SET;
        }
        save();
    }

    /**
     * Records the correlation value for an alpha.
     */
    public void recordCorrelation(String alphaId, Double correlation) {
        AlphaProgress progress = data.alphas.computeIfAbsent(alphaId, AlphaProgress::new);
        progress.correlation = correlation;
        if (progress.status.ordinal() < AlphaStatus.CORR_CHECKED.ordinal()) {
            progress.status = AlphaStatus.CORR_CHECKED;
        }
        save();
    }

    /**
     * Marks that an alpha has been favorited.
     */
    public void markFavorited(String alphaId) {
        AlphaProgress progress = data.alphas.computeIfAbsent(alphaId, AlphaProgress::new);
        if (progress.status.ordinal() < AlphaStatus.FAVORITED.ordinal()) {
            progress.status = AlphaStatus.FAVORITED;
        }
        save();
    }

    /**
     * Marks that an alpha has been submitted.
     */
    public void markSubmitted(String alphaId, boolean success) {
        AlphaProgress progress = data.alphas.computeIfAbsent(alphaId, AlphaProgress::new);
        progress.submitted = success;
        if (success) {
            progress.status = AlphaStatus.SUBMITTED;
        }
        save();
    }

    /**
     * Marks an alpha as completed.
     */
    public void markCompleted(String alphaId) {
        updateStatus(alphaId, AlphaStatus.COMPLETED);
    }

    /**
     * Marks an alpha as failed with an error message.
     */
    public void markFailed(String alphaId, String errorMessage) {
        AlphaProgress progress = data.alphas.computeIfAbsent(alphaId, AlphaProgress::new);
        progress.status = AlphaStatus.FAILED;
        progress.errorMessage = errorMessage;
        progress.completedAt = Instant.now().toString();
        data.processedCount++;
        data.failedCount++;
        save();
    }

    /**
     * Marks an alpha as skipped.
     */
    public void markSkipped(String alphaId, String reason) {
        AlphaProgress progress = data.alphas.computeIfAbsent(alphaId, AlphaProgress::new);
        progress.status = AlphaStatus.SKIPPED;
        progress.errorMessage = reason;
        progress.completedAt = Instant.now().toString();
        data.processedCount++;
        data.successCount++;
        save();
    }

    /**
     * Gets all alphas that still need processing.
     */
    public List<String> getPendingAlphas(List<String> allAlphaIds) {
        return allAlphaIds.stream()
                .filter(id -> !isAlreadyProcessed(id))
                .collect(Collectors.toList());
    }

    /**
     * Gets all correlation values that have been recorded.
     */
    public Map<String, Double> getAllCorrelations() {
        Map<String, Double> correlations = new HashMap<>();
        for (Map.Entry<String, AlphaProgress> entry : data.alphas.entrySet()) {
            if (entry.getValue().correlation != null) {
                correlations.put(entry.getKey(), entry.getValue().correlation);
            }
        }
        return correlations;
    }

    /**
     * Gets alphas that need submission (correlation checked but not yet submitted).
     */
    public List<String> getAlphasToSubmit() {
        return data.alphas.entrySet().stream()
                .filter(e -> e.getValue().correlation != null && !e.getValue().submitted)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Saves progress to file.
     */
    public void save() {
        synchronized (saveLock) {
            try {
                data.lastUpdatedAt = Instant.now().toString();
                mapper.writeValue(new File(progressFilePath), data);
            } catch (IOException e) {
                System.err.println("Failed to save progress: " + e.getMessage());
            }
        }
    }

    /**
     * Clears all progress and deletes the progress file.
     * Use this to start fresh.
     */
    public void clear() {
        data.alphas.clear();
        data.processedCount = 0;
        data.successCount = 0;
        data.failedCount = 0;
        data.startedAt = Instant.now().toString();

        try {
            Files.deleteIfExists(Paths.get(progressFilePath));
            System.out.println("Progress cleared and file deleted: " + progressFilePath);
        } catch (IOException e) {
            System.err.println("Failed to delete progress file: " + e.getMessage());
        }
    }

    /**
     * Gets a summary of the current progress.
     */
    public String getSummary() {
        int pending = 0;
        int inProgress = 0;
        int completed = 0;
        int failed = 0;
        int skipped = 0;

        for (AlphaProgress progress : data.alphas.values()) {
            switch (progress.status) {
                case PENDING -> pending++;
                case NAME_SET, CORR_CHECKED, FAVORITED -> inProgress++;
                case SUBMITTED, COMPLETED -> completed++;
                case FAILED -> failed++;
                case SKIPPED -> skipped++;
            }
        }

        return String.format(
                "Progress: Total=%d, Pending=%d, InProgress=%d, Completed=%d, Failed=%d, Skipped=%d",
                data.totalAlphas, pending, inProgress, completed, failed, skipped
        );
    }

    /**
     * Prints detailed progress information.
     */
    public void printProgress() {
        System.out.println("========== PROGRESS REPORT ==========");
        System.out.println("Job Type: " + data.jobType);
        System.out.println("Started: " + data.startedAt);
        System.out.println("Last Updated: " + data.lastUpdatedAt);
        System.out.println(getSummary());
        System.out.println("=====================================");
    }

    /**
     * Gets the progress file path.
     */
    public String getProgressFilePath() {
        return progressFilePath;
    }

    /**
     * Checks if there is existing progress to resume.
     */
    public boolean hasExistingProgress() {
        return !data.alphas.isEmpty() && data.processedCount > 0;
    }

    /**
     * Gets the number of already processed alphas.
     */
    public int getProcessedCount() {
        return data.processedCount;
    }

    /**
     * Gets the total number of alphas.
     */
    public int getTotalCount() {
        return data.totalAlphas;
    }
}
