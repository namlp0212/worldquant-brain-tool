package demo.webapp.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import demo.webapp.ConfigLoader;
import demo.webapp.ProgressTracker;
import demo.webapp.SessionValidator;
import demo.webapp.regular.RegularAlphaUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * REST API handlers for the web dashboard.
 */
public class ApiHandler {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Track running jobs
    private static final Map<String, JobInfo> runningJobs = new ConcurrentHashMap<>();
    private static final ExecutorService jobExecutor = Executors.newFixedThreadPool(3);

    public static class JobInfo {
        public String id;
        public String type;
        public String status; // RUNNING, COMPLETED, FAILED
        public String startedAt;
        public String completedAt;
        public String error;

        public JobInfo(String id, String type) {
            this.id = id;
            this.type = type;
            this.status = "RUNNING";
            this.startedAt = Instant.now().toString();
        }
    }

    /**
     * GET/POST /api/config - Get or update configuration
     */
    public static class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    // Return current config (without sensitive data)
                    ObjectNode config = mapper.createObjectNode();
                    config.put("smtpHost", ConfigLoader.getSmtpHost());
                    config.put("smtpPort", ConfigLoader.getSmtpPort());
                    config.put("smtpUsername", ConfigLoader.getSmtpUsername());
                    config.put("emailRecipient", ConfigLoader.getEmailRecipient());
                    config.put("threadPoolSize", ConfigLoader.getThreadPoolSize());
                    config.put("minCorrelation", ConfigLoader.getMinCorrelation());
                    config.put("minFitness", ConfigLoader.getMinFitness());

                    sendJson(exchange, 200, config);

                } else if ("POST".equals(exchange.getRequestMethod())) {
                    // Update config file
                    String body = readRequestBody(exchange);
                    JsonNode updates = mapper.readTree(body);

                    // Read current config
                    Path configPath = Paths.get("config.properties");
                    Properties props = new Properties();
                    if (Files.exists(configPath)) {
                        try (InputStream is = Files.newInputStream(configPath)) {
                            props.load(is);
                        }
                    }

                    // Apply updates
                    updates.fields().forEachRemaining(field -> {
                        String key = field.getKey();
                        String value = field.getValue().asText();
                        props.setProperty(key, value);
                    });

                    // Save config
                    try (OutputStream os = Files.newOutputStream(configPath)) {
                        props.store(os, "Updated via Web Dashboard");
                    }

                    sendJson(exchange, 200, mapper.createObjectNode().put("success", true));
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * GET/POST /api/session - Get session status or update cookie
     */
    public static class SessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    // Validate current session
                    SessionValidator.ValidationResult result = SessionValidator.validate();

                    ObjectNode response = mapper.createObjectNode();
                    response.put("valid", result.isValid());
                    response.put("message", result.getMessage());
                    if (result.isValid()) {
                        response.put("userId", result.getUserId());
                        response.put("email", result.getEmail());
                        response.put("username", result.getUsername());
                    }

                    // Get masked cookie
                    String cookie = ConfigLoader.getCookie();
                    if (cookie != null && cookie.length() > 50) {
                        response.put("cookiePreview", cookie.substring(0, 30) + "..." + cookie.substring(cookie.length() - 20));
                    } else {
                        response.put("cookiePreview", cookie != null ? cookie : "Not set");
                    }

                    sendJson(exchange, 200, response);

                } else if ("POST".equals(exchange.getRequestMethod())) {
                    // Update cookie
                    String body = readRequestBody(exchange);
                    JsonNode json = mapper.readTree(body);
                    String newCookie = json.get("cookie").asText();

                    // Update config file
                    Path configPath = Paths.get("config.properties");
                    Properties props = new Properties();
                    if (Files.exists(configPath)) {
                        try (InputStream is = Files.newInputStream(configPath)) {
                            props.load(is);
                        }
                    }

                    props.setProperty("wq.cookie", newCookie);

                    try (OutputStream os = Files.newOutputStream(configPath)) {
                        props.store(os, "Cookie updated via Web Dashboard at " + Instant.now());
                    }

                    // Reload config to apply changes immediately
                    ConfigLoader.reload();
                    demo.webapp.Constant.refreshCookie();

                    // Validate the new session immediately
                    SessionValidator.ValidationResult validation = SessionValidator.validate();

                    ObjectNode response = mapper.createObjectNode();
                    response.put("success", true);
                    response.put("sessionValid", validation.isValid());
                    if (validation.isValid()) {
                        response.put("message", "Cookie updated and session is valid!");
                        response.put("username", validation.getUsername());
                    } else {
                        response.put("message", "Cookie updated but session is invalid: " + validation.getMessage());
                    }

                    sendJson(exchange, 200, response);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * GET /api/jobs - Get running and recent jobs
     */
    public static class JobsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed");
                    return;
                }

                ArrayNode jobsArray = mapper.createArrayNode();

                for (JobInfo job : runningJobs.values()) {
                    ObjectNode jobNode = mapper.createObjectNode();
                    jobNode.put("id", job.id);
                    jobNode.put("type", job.type);
                    jobNode.put("status", job.status);
                    jobNode.put("startedAt", job.startedAt);
                    if (job.completedAt != null) {
                        jobNode.put("completedAt", job.completedAt);
                    }
                    if (job.error != null) {
                        jobNode.put("error", job.error);
                    }
                    jobsArray.add(jobNode);
                }

                ObjectNode response = mapper.createObjectNode();
                response.set("jobs", jobsArray);
                response.put("count", runningJobs.size());

                sendJson(exchange, 200, response);

            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * GET /api/results - Get historical results from progress files
     */
    public static class ResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed");
                    return;
                }

                ObjectNode response = mapper.createObjectNode();

                // Load progress files
                String[] progressFiles = {"progress_regular.json", "progress_super.json", "progress_regular_gen_super.json"};

                for (String filename : progressFiles) {
                    Path path = Paths.get(filename);
                    if (Files.exists(path)) {
                        try {
                            String content = Files.readString(path);
                            JsonNode progressData = mapper.readTree(content);
                            response.set(filename.replace(".json", ""), progressData);
                        } catch (Exception e) {
                            response.put(filename, "Error reading: " + e.getMessage());
                        }
                    }
                }

                sendJson(exchange, 200, response);

            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * GET /api/progress/:type - Get progress for specific job type
     */
    public static class ProgressHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed");
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String type = path.substring("/api/progress/".length());

                if (type.isEmpty()) {
                    type = "regular";
                }

                String filename = "progress_" + type.toLowerCase() + ".json";
                Path progressPath = Paths.get(filename);

                if (Files.exists(progressPath)) {
                    String content = Files.readString(progressPath);
                    JsonNode progressData = mapper.readTree(content);
                    sendJson(exchange, 200, progressData);
                } else {
                    ObjectNode response = mapper.createObjectNode();
                    response.put("message", "No progress file found for type: " + type);
                    response.put("file", filename);
                    sendJson(exchange, 404, response);
                }

            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * POST /api/run - Trigger a manual job run
     */
    public static class RunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed");
                    return;
                }

                String body = readRequestBody(exchange);
                JsonNode json = mapper.readTree(body);
                String jobType = json.get("type").asText();
                boolean clearProgress = json.has("clear") && json.get("clear").asBoolean();

                // Validate session first
                SessionValidator.ValidationResult validation = SessionValidator.validate();
                if (!validation.isValid()) {
                    ObjectNode response = mapper.createObjectNode();
                    response.put("success", false);
                    response.put("error", "Session invalid: " + validation.getMessage());
                    sendJson(exchange, 400, response);
                    return;
                }

                // Create job
                String jobId = UUID.randomUUID().toString().substring(0, 8);
                JobInfo jobInfo = new JobInfo(jobId, jobType);
                runningJobs.put(jobId, jobInfo);

                // Run job asynchronously
                String[] args = clearProgress ? new String[]{"--clear"} : new String[]{};

                jobExecutor.submit(() -> {
                    try {
                        switch (jobType.toLowerCase()) {
                            case "regular" -> demo.webapp.regular.RegularAlphaUtils.main(args);
                            case "super" -> demo.webapp.regular.SuperAlphaUtils.main(args);
                            case "gen-super", "regular_gen_super" ->
                                    demo.webapp.regular.RegularAlphaForGenSuperAlphaUtils.main(args);
                            default -> throw new IllegalArgumentException("Unknown job type: " + jobType);
                        }
                        jobInfo.status = "COMPLETED";
                    } catch (Exception e) {
                        jobInfo.status = "FAILED";
                        jobInfo.error = e.getMessage();
                        e.printStackTrace();
                    } finally {
                        jobInfo.completedAt = Instant.now().toString();
                    }
                });

                ObjectNode response = mapper.createObjectNode();
                response.put("success", true);
                response.put("jobId", jobId);
                response.put("message", "Job started: " + jobType);

                sendJson(exchange, 200, response);

            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * GET/POST /api/filters - Get or update filter settings
     */
    public static class FiltersHandler implements HttpHandler {

        // Predefined region options
        private static final String[] REGIONS = {"JPN", "USA", "EUR", "ASI", "IND", "CHN", "KOR", "TWN", "GLB"};

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    // Return current filter settings
                    ObjectNode response = mapper.createObjectNode();

                    // Regular alpha filters
                    ObjectNode regularFilters = mapper.createObjectNode();
                    regularFilters.put("region", ConfigLoader.getRegularFilterRegion());
                    regularFilters.put("dateFrom", ConfigLoader.getRegularFilterDateFrom());
                    regularFilters.put("dateTo", ConfigLoader.getRegularFilterDateTo());
                    regularFilters.put("minFitness", ConfigLoader.getRegularFilterMinFitness());
                    regularFilters.put("limit", ConfigLoader.getRegularFilterLimit());
                    regularFilters.put("status", ConfigLoader.getRegularFilterStatus());
                    regularFilters.put("favorite", ConfigLoader.getRegularFilterFavorite());

                    response.set("regular", regularFilters);

                    // Super alpha filters
                    ObjectNode superFilters = mapper.createObjectNode();
                    superFilters.put("region", ConfigLoader.getSuperFilterRegion());
                    superFilters.put("dateFrom", ConfigLoader.getSuperFilterDateFrom());
                    superFilters.put("dateTo", ConfigLoader.getSuperFilterDateTo());
                    superFilters.put("limit", ConfigLoader.getSuperFilterLimit());
                    superFilters.put("status", ConfigLoader.getSuperFilterStatus());
                    superFilters.put("favorite", ConfigLoader.getSuperFilterFavorite());

                    response.set("super", superFilters);

                    // Available regions
                    ArrayNode regionsArray = mapper.createArrayNode();
                    for (String region : REGIONS) {
                        regionsArray.add(region);
                    }
                    // Add custom regions from config if any
                    String customRegions = ConfigLoader.get("filter.custom.regions", "");
                    if (!customRegions.isBlank()) {
                        for (String region : customRegions.split(",")) {
                            if (!region.isBlank()) {
                                regionsArray.add(region.trim());
                            }
                        }
                    }
                    response.set("availableRegions", regionsArray);

                    sendJson(exchange, 200, response);

                } else if ("POST".equals(exchange.getRequestMethod())) {
                    // Update filter settings
                    String body = readRequestBody(exchange);
                    JsonNode json = mapper.readTree(body);

                    // Read current config
                    Path configPath = Paths.get("config.properties");
                    Properties props = new Properties();
                    if (Files.exists(configPath)) {
                        try (InputStream is = Files.newInputStream(configPath)) {
                            props.load(is);
                        }
                    }

                    // Check if this is a super alpha filter update
                    boolean isSuperFilter = json.has("type") && "super".equals(json.get("type").asText());

                    if (isSuperFilter) {
                        // Update super alpha filters
                        if (json.has("region")) {
                            props.setProperty("filter.super.region", json.get("region").asText());
                        }
                        if (json.has("dateFrom")) {
                            props.setProperty("filter.super.date.from", json.get("dateFrom").asText());
                        }
                        if (json.has("dateTo")) {
                            props.setProperty("filter.super.date.to", json.get("dateTo").asText());
                        }
                        if (json.has("limit")) {
                            props.setProperty("filter.super.limit", json.get("limit").asText());
                        }
                        if (json.has("favorite")) {
                            props.setProperty("filter.super.favorite", json.get("favorite").asText());
                        }
                    } else {
                        // Update regular filters
                        if (json.has("region")) {
                            props.setProperty("filter.regular.region", json.get("region").asText());
                        }
                        if (json.has("dateFrom")) {
                            props.setProperty("filter.regular.date.from", json.get("dateFrom").asText());
                        }
                        if (json.has("dateTo")) {
                            props.setProperty("filter.regular.date.to", json.get("dateTo").asText());
                        }
                        if (json.has("minFitness")) {
                            props.setProperty("filter.regular.min.fitness", json.get("minFitness").asText());
                        }
                        if (json.has("limit")) {
                            props.setProperty("filter.regular.limit", json.get("limit").asText());
                        }
                        if (json.has("status")) {
                            props.setProperty("filter.regular.status", json.get("status").asText());
                        }
                        if (json.has("favorite")) {
                            props.setProperty("filter.regular.favorite", json.get("favorite").asText());
                        }
                    }

                    // Handle custom region (only for regular filters)
                    if (json.has("customRegion") && !json.get("customRegion").asText().isBlank()) {
                        String newRegion = json.get("customRegion").asText().trim().toUpperCase();
                        String existingCustom = props.getProperty("filter.custom.regions", "");
                        if (!existingCustom.contains(newRegion)) {
                            if (existingCustom.isBlank()) {
                                props.setProperty("filter.custom.regions", newRegion);
                            } else {
                                props.setProperty("filter.custom.regions", existingCustom + "," + newRegion);
                            }
                        }
                    }

                    // Save config
                    try (OutputStream os = Files.newOutputStream(configPath)) {
                        props.store(os, "Filters updated via Web Dashboard at " + Instant.now());
                    }

                    // Reload config
                    ConfigLoader.reload();

                    ObjectNode response = mapper.createObjectNode();
                    response.put("success", true);
                    response.put("message", "Filters updated successfully");

                    sendJson(exchange, 200, response);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * Handles manual alpha submission requests.
     * POST /api/submit - Submit alphas manually
     */
    public static class SubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed");
                    return;
                }

                String body = readRequestBody(exchange);
                JsonNode json = mapper.readTree(body);

                // Get alpha IDs to submit
                List<String> alphaIds = new ArrayList<>();
                if (json.has("alphaIds") && json.get("alphaIds").isArray()) {
                    for (JsonNode node : json.get("alphaIds")) {
                        alphaIds.add(node.asText());
                    }
                }

                if (alphaIds.isEmpty()) {
                    sendError(exchange, 400, "No alpha IDs provided");
                    return;
                }

                // Load progress tracker
                ProgressTracker progress = new ProgressTracker("regular");

                // Submit each alpha
                ObjectNode response = mapper.createObjectNode();
                ArrayNode results = mapper.createArrayNode();
                int successCount = 0;
                int failCount = 0;

                for (String alphaId : alphaIds) {
                    ObjectNode alphaResult = mapper.createObjectNode();
                    alphaResult.put("alphaId", alphaId);

                    try {
                        boolean success = RegularAlphaUtils.submitAlpha(alphaId);
                        alphaResult.put("success", success);

                        if (success) {
                            successCount++;
                            progress.markSubmitted(alphaId, true);
                            alphaResult.put("message", "Submitted successfully");
                        } else {
                            failCount++;
                            progress.markSubmitted(alphaId, false);
                            alphaResult.put("message", "Submission failed");
                        }
                    } catch (Exception e) {
                        failCount++;
                        alphaResult.put("success", false);
                        alphaResult.put("message", "Error: " + e.getMessage());
                    }

                    results.add(alphaResult);
                }

                response.put("success", failCount == 0);
                response.put("submitted", successCount);
                response.put("failed", failCount);
                response.set("results", results);

                sendJson(exchange, 200, response);

            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * Handles log retrieval requests.
     * GET /api/logs - Get application logs
     */
    public static class LogsHandler implements HttpHandler {

        // Ring buffer to store recent log lines
        private static final int MAX_LOG_LINES = 5000;
        private static final List<String> logBuffer = Collections.synchronizedList(new LinkedList<>());
        private static final List<LogSubscriber> subscribers = Collections.synchronizedList(new ArrayList<>());
        private static boolean initialized = false;

        public static class LogSubscriber {
            public OutputStream outputStream;
            public String filter;
            public volatile boolean active = true;

            public LogSubscriber(OutputStream os, String filter) {
                this.outputStream = os;
                this.filter = filter;
            }
        }

        public static void initLogCapture() {
            if (initialized) return;
            initialized = true;

            // Capture System.out
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(originalOut) {
                @Override
                public void println(String x) {
                    super.println(x);
                    addLog(x, false);
                }
                @Override
                public void println(Object x) {
                    super.println(x);
                    addLog(String.valueOf(x), false);
                }
            });

            // Capture System.err
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(originalErr) {
                @Override
                public void println(String x) {
                    super.println(x);
                    addLog(x, true);
                }
                @Override
                public void println(Object x) {
                    super.println(x);
                    addLog(String.valueOf(x), true);
                }
            });
        }

        private static void addLog(String line, boolean isError) {
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logLine = "[" + timestamp + "] " + (isError ? "[ERROR] " : "") + line;
            logBuffer.add(logLine);
            while (logBuffer.size() > MAX_LOG_LINES) {
                logBuffer.remove(0);
            }

            // Notify all SSE subscribers
            notifySubscribers(logLine);
        }

        private static void notifySubscribers(String logLine) {
            List<LogSubscriber> deadSubscribers = new ArrayList<>();

            synchronized (subscribers) {
                for (LogSubscriber sub : subscribers) {
                    if (!sub.active) {
                        deadSubscribers.add(sub);
                        continue;
                    }

                    // Check filter
                    if (!matchesFilter(logLine, sub.filter)) {
                        continue;
                    }

                    try {
                        String sseData = "data: " + logLine.replace("\n", "\\n") + "\n\n";
                        sub.outputStream.write(sseData.getBytes(StandardCharsets.UTF_8));
                        sub.outputStream.flush();
                    } catch (Exception e) {
                        sub.active = false;
                        deadSubscribers.add(sub);
                    }
                }

                subscribers.removeAll(deadSubscribers);
            }
        }

        private static boolean matchesFilter(String log, String filter) {
            switch (filter) {
                case "regular":
                    return log.contains("Regular") || log.contains("regular") ||
                           (log.contains("alpha") && !log.contains("Super") && !log.contains("super"));
                case "super":
                    return log.contains("Super") || log.contains("super");
                case "error":
                    return log.contains("ERROR") || log.contains("❌") ||
                           log.contains("⛔") || log.contains("Exception") ||
                           log.contains("FAIL");
                default:
                    return true;
            }
        }

        public static List<String> getFilteredLogs(String filter, int lines) {
            List<String> filteredLogs = new ArrayList<>();
            synchronized (logBuffer) {
                for (String log : logBuffer) {
                    if (matchesFilter(log, filter)) {
                        filteredLogs.add(log);
                    }
                }
            }
            int startIndex = Math.max(0, filteredLogs.size() - lines);
            return filteredLogs.subList(startIndex, filteredLogs.size());
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendError(exchange, 405, "Method not allowed");
                    return;
                }

                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                int lines = 500;
                String filter = "all";

                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] parts = param.split("=");
                        if (parts.length == 2) {
                            if ("lines".equals(parts[0])) {
                                lines = Integer.parseInt(parts[1]);
                            } else if ("filter".equals(parts[0])) {
                                filter = parts[1];
                            }
                        }
                    }
                }

                List<String> result = getFilteredLogs(filter, lines);

                ObjectNode response = mapper.createObjectNode();
                ArrayNode logsArray = mapper.createArrayNode();
                for (String log : result) {
                    logsArray.add(log);
                }
                response.set("logs", logsArray);
                response.put("total", logBuffer.size());
                response.put("returned", result.size());

                sendJson(exchange, 200, response);

            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * Handles real-time log streaming via Server-Sent Events (SSE).
     * GET /api/logs/stream - Stream logs in real-time
     */
    public static class LogStreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            // Parse filter parameter
            String query = exchange.getRequestURI().getQuery();
            String filter = "all";
            int initialLines = 100;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] parts = param.split("=");
                    if (parts.length == 2) {
                        if ("filter".equals(parts[0])) {
                            filter = parts[1];
                        } else if ("initial".equals(parts[0])) {
                            initialLines = Integer.parseInt(parts[1]);
                        }
                    }
                }
            }

            // Set SSE headers
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();

            // Send initial logs
            List<String> initialLogs = LogsHandler.getFilteredLogs(filter, initialLines);
            for (String log : initialLogs) {
                String sseData = "data: " + log.replace("\n", "\\n") + "\n\n";
                os.write(sseData.getBytes(StandardCharsets.UTF_8));
            }
            os.flush();

            // Register subscriber for new logs
            LogsHandler.LogSubscriber subscriber = new LogsHandler.LogSubscriber(os, filter);
            LogsHandler.subscribers.add(subscriber);

            // Keep connection alive
            try {
                while (subscriber.active) {
                    Thread.sleep(1000);
                    // Send heartbeat to detect disconnection
                    try {
                        os.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    } catch (IOException e) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                subscriber.active = false;
                LogsHandler.subscribers.remove(subscriber);
                try {
                    os.close();
                } catch (IOException ignored) {}
            }
        }
    }

    // Helper methods

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, JsonNode json) throws IOException {
        String response = mapper.writeValueAsString(json);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        ObjectNode error = mapper.createObjectNode();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }
}
