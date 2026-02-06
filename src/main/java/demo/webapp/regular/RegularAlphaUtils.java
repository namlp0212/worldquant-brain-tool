package demo.webapp.regular;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.webapp.regular.entity.alphaIdResponse.AlphaItem;
import demo.webapp.regular.entity.alphaIdResponse.AlphaListResponse;
import demo.webapp.regular.entity.productCorrResponse.CorrelationBucket;
import demo.webapp.regular.entity.productCorrResponse.CorrelationResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import demo.webapp.ConfigLoader;
import demo.webapp.ProgressTracker;
import demo.webapp.SessionValidator;

import static demo.webapp.Constant.COOKIE;

public class RegularAlphaUtils {

    public static Double getProdCorrOfAlpha(String alphaId) throws InterruptedException, IOException {
        System.out.println("----- START CHECK PRODUCT CORR OF ALPHA " + alphaId + " ------");

        String url = "https://api.worldquantbrain.com/alphas/"
                + alphaId + "/correlations/prod";

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        int maxRetry = 500;
        int retry = 0;
        int waitMs = 100; // 2s ban đầu

        while (retry < maxRetry) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Referer",
                            "https://platform.worldquantbrain.com/")
                    .header("Cookie", COOKIE)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String body = response.body();

            System.out.println("Alpha " + alphaId
                    + " | Attempt " + (retry + 1)
                    + " | Status " + status);

            // ✅ CASE 1: OK + CÓ BODY
            if (status == 200 && body != null && !body.isBlank()) {
                CorrelationResponse cr =
                        mapper.readValue(body, CorrelationResponse.class);
                return cr.getMax();
            }

            // 🔁 CASE 2: OK + BODY RỖNG → retry
            if (status == 200 && (body == null || body.isBlank())) {
                System.out.println("Prod corr not ready yet. Retry after "
                        + waitMs + "ms...");
                Thread.sleep(waitMs);
//                waitMs *= 2;
                retry++;
                continue;
            }

            // 🚫 CASE 3: RATE LIMIT
            if (status == 429) {
                System.out.println("Rate limited. Retry after "
                        + waitMs + "ms...");
                Thread.sleep(waitMs);
                waitMs += 10;
                retry++;
                continue;
            }

            // ❌ CASE 4: lỗi khác → dừng
            System.out.println("Non-retryable error: " + status);
            break;
        }

        System.out.println("Alpha " + alphaId
                + " has no prod correlation after max retries");

        return null;
    }

    public static List<String> getAlphaIdsByFilter() throws IOException, InterruptedException {
        // Load filter settings from config
        String region = ConfigLoader.getRegularFilterRegion();
        String dateFrom = ConfigLoader.getRegularFilterDateFrom();
        String dateTo = ConfigLoader.getRegularFilterDateTo();
        double minFitness = ConfigLoader.getRegularFilterMinFitness();
        int limit = ConfigLoader.getRegularFilterLimit();
        String filterStatus = ConfigLoader.getRegularFilterStatus();
        boolean favorite = ConfigLoader.getRegularFilterFavorite();

        String url = "https://api.worldquantbrain.com/users/self/alphas" +
                "?limit=" + limit +
                "&offset=0" +
                "&status=" + filterStatus +
                "&dateCreated%3E=" + dateFrom +
                "&dateCreated%3C" + dateTo +
                "&type=REGULAR" +
                "&settings.region=" + region +
                "&is.fitness%3E=" + minFitness +
                "&favorite=" + favorite +
                "&order=-is.fitness" +
                "&hidden=false";

        System.out.println("Filter URL: " + url);

        HttpClient client = HttpClient.newHttpClient();

        int maxRetry = 5;
        int retry = 0;
        int waitMs = 2000; // 2 giây

        while (retry < maxRetry) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Referer",
                            "https://platform.worldquantbrain.com/")
                    .header("Cookie", COOKIE)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            System.out.println("Attempt " + (retry + 1)
                    + " | Status: " + status);

            if (status == 200) {
                System.out.println("SUCCESS");

                String body = response.body();
//                System.out.println("body: " + body);

                ObjectMapper mapper = new ObjectMapper();

                AlphaListResponse alphaListResponse =
                        mapper.readValue(body, AlphaListResponse.class);

                return alphaListResponse.getResults().stream().map(AlphaItem::getId).collect(Collectors.toList());
            }

            if (status == 429) {
                System.out.println("Rate limited. Waiting "
                        + waitMs + "ms...");
                Thread.sleep(waitMs);
                waitMs *= 2; // exponential backoff
            } else {
                System.out.println("Non-retryable error: " + status);
                break;
            }

            retry++;
        }

        if (retry == maxRetry) {
            System.out.println("Failed after max retries");
        }

        return null;
    }

    public static void setNameForAlpha(String alphaId) throws IOException, InterruptedException {
        System.out.println("---- START SET NAME FOR ALPHA + " + alphaId + " -----");

        String url = "https://api.worldquantbrain.com/alphas/" + alphaId;

        String jsonBody = String.format("""
            {
              "name": "%s",
              "color": null,
              "category": null,
              "tags": [],
              "regular": {
                "description": null
              },
              "osmosisPoints": null
            }
            """, alphaId);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        int maxRetry = 5;
        int retry = 0;
        int waitMs = 2000; // 2s ban đầu

        while (retry < maxRetry) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://platform.worldquantbrain.com")
                    .header("Referer", "https://platform.worldquantbrain.com/")
                    .header("Cookie", COOKIE)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();

            System.out.println("Set name alpha " + alphaId
                    + " | Attempt " + (retry + 1)
                    + " | Status " + status);

            // ✅ SUCCESS
            if (status == 200) {
                System.out.println("Set name SUCCESS for alpha " + alphaId);
                return;
            }

            // 🔁 RETRY
            retry++;

            if (retry < maxRetry) {
                System.out.println("Retry after " + waitMs + "ms...");
                Thread.sleep(waitMs);
                waitMs *= 2;
            }
        }

        System.out.println("---- END SET NAME FOR ALPHA + " + alphaId + " -----");

        // ❌ FAIL SAU KHI RETRY HẾT
        throw new RuntimeException(
                "Failed to set name for alpha " + alphaId
                        + " after " + maxRetry + " retries"
        );
    }

    private static void sendResultByMail(String result) {
        EmailSender emailSender = new EmailSender(
                ConfigLoader.getSmtpHost(),
                ConfigLoader.getSmtpPort(),
                ConfigLoader.getSmtpUsername(),
                ConfigLoader.getSmtpPassword()
        );

        Date now = new Date();
        result = "Result " + now + "\n" + result;
        try {
            emailSender.sendEmail(
                    ConfigLoader.getEmailRecipient(),
                    "Result of run REGULAR alpha: ",
                    result
            );
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean submitAlpha(String alphaId) throws IOException, InterruptedException {
        System.out.println("---- START SUBMIT ALPHA " + alphaId + " -----");

        String url = "https://api.worldquantbrain.com/alphas/" + alphaId + "/submit";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        int maxRetry = 100;
        int retry = 0;
        int waitMs = 2000; // 2s initial wait

        while (retry < maxRetry) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://platform.worldquantbrain.com")
                    .header("Referer", "https://platform.worldquantbrain.com/")
                    .header("Cookie", COOKIE)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String body = response.body();

            System.out.println("Submit alpha " + alphaId
                    + " | Attempt " + (retry + 1)
                    + " | Status " + status);

            // SUCCESS
            if (status == 200 || status == 201) {
                System.out.println("✔ SUBMIT ALPHA " + alphaId + " SUCCESS");
                System.out.println("Body: " + body);
                return true;
            }

            // RATE LIMIT - retry with backoff
            if (status == 429) {
                System.out.println("Rate limited. Retry after " + waitMs + "ms...");
                Thread.sleep(waitMs);
                waitMs *= 2;
                retry++;
                continue;
            }

            // SERVER ERROR (5xx) - retry with backoff
            if (status >= 500 && status < 600) {
                System.out.println("Server error. Retry after " + waitMs + "ms...");
                Thread.sleep(waitMs);
                waitMs *= 2;
                retry++;
                continue;
            }

            // CLIENT ERROR (4xx except 429) - non-retryable, fail immediately
            if (status >= 400 && status < 500) {
                System.out.println("❌ SUBMIT ALPHA " + alphaId + " FAIL - Client error: " + status);
                System.out.println("Body: " + body);
                return false;
            }

            // Other unexpected status - retry
            System.out.println("Unexpected status " + status + ". Retry after " + waitMs + "ms...");
            Thread.sleep(waitMs);
            waitMs *= 2;
            retry++;
        }

        System.out.println("❌ SUBMIT ALPHA " + alphaId + " FAIL after " + maxRetry + " retries");
        return false;
    }

    public static void setFavoriteForAlpha(String alphaId) throws IOException, InterruptedException {
        System.out.println("---- START SET FAVORITE FOR ALPHA + " + alphaId + " -----");

        String url = "https://api.worldquantbrain.com/alphas/" + alphaId;

        String jsonBody = String.format("""
            {"favorite":true}
            """, alphaId);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        int maxRetry = 5;
        int retry = 0;
        int waitMs = 2000; // 2s ban đầu

        while (retry < maxRetry) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://platform.worldquantbrain.com")
                    .header("Referer", "https://platform.worldquantbrain.com/")
                    .header("Cookie", COOKIE)
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();

            System.out.println("Set FAVORITE alpha " + alphaId
                    + " | Attempt " + (retry + 1)
                    + " | Status " + status);

            // ✅ SUCCESS
            if (status == 200) {
                System.out.println("Set FAVORITE SUCCESS for alpha " + alphaId);
                return;
            }

            // 🔁 RETRY
            retry++;

            if (retry < maxRetry) {
                System.out.println("Retry after " + waitMs + "ms...");
                Thread.sleep(waitMs);
                waitMs *= 2;
            }
        }

        System.out.println("---- END SET FAVORITE FOR ALPHA + " + alphaId + " -----");

        // ❌ FAIL SAU KHI RETRY HẾT
        throw new RuntimeException(
                "Failed to set FAVORITE for alpha " + alphaId
                        + " after " + maxRetry + " retries"
        );
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Check for --clear flag to reset progress
        boolean clearProgress = args.length > 0 && args[0].equals("--clear");

        // Validate session before starting
        try {
            SessionValidator.validateOrThrow();
        } catch (SessionValidator.SessionInvalidException e) {
            System.err.println("Cannot proceed: " + e.getMessage());
            return;
        }

        // Initialize progress tracker
        ProgressTracker progress = new ProgressTracker("REGULAR");

        if (clearProgress) {
            System.out.println("Clearing previous progress...");
            progress.clear();
            progress = new ProgressTracker("REGULAR");
        }

        if (progress.hasExistingProgress()) {
            System.out.println("Resuming from previous progress...");
            progress.printProgress();
        }

        ExecutorService executor = Executors.newFixedThreadPool(ConfigLoader.getThreadPoolSize());

        Map<String, Double> mapProductCorrByAlphaId = Collections.synchronizedMap(new HashMap<>());

        // Load cached correlations from progress
        mapProductCorrByAlphaId.putAll(progress.getAllCorrelations());

        System.out.println("------------------ START GET LIST ALPHA ID -------------------------");
        List<String> alphaIds = getAlphaIdsByFilter();
        System.out.println("------------------ END GET LIST ALPHA ID -------------------------");

        if (alphaIds == null || alphaIds.isEmpty()) {
            System.out.println("Alpha list is empty or null");
            executor.shutdown();
            return;
        }

        System.out.println("Alpha list from API: " + alphaIds.size());

        // Initialize progress tracking for all alphas
        progress.initializeAlphas(alphaIds);

        // Filter out already processed alphas
        List<String> pendingAlphaIds = progress.getPendingAlphas(alphaIds);
        System.out.println("Alphas to process (excluding already completed): " + pendingAlphaIds.size());

        if (pendingAlphaIds.isEmpty()) {
            System.out.println("All alphas already processed. Use --clear flag to start fresh.");
            executor.shutdown();

            // Still proceed to submission phase for any unsubmitted alphas
            submitPendingAlphas(progress, mapProductCorrByAlphaId);
            return;
        }

        List<Future<?>> futures = new ArrayList<>();
        final ProgressTracker progressFinal = progress;

        for (String alphaId : pendingAlphaIds) {
            Future<?> future = executor.submit(() -> {
                try {
                    System.out.println("▶ START alpha " + alphaId
                            + " | Thread " + Thread.currentThread().getName());

                    // 1️⃣ set name (skip if already done)
                    if (!progressFinal.isNameSet(alphaId)) {
                        setNameForAlpha(alphaId);
                        progressFinal.markNameSet(alphaId);
                    } else {
                        System.out.println("Skipping name set for " + alphaId + " (already done)");
                    }

                    // 2️⃣ get product corr (skip if already done)
                    Double productCorr;
                    if (!progressFinal.isCorrelationChecked(alphaId)) {
                        productCorr = getProdCorrOfAlpha(alphaId);
                        progressFinal.recordCorrelation(alphaId, productCorr);
                    } else {
                        productCorr = progressFinal.getCorrelation(alphaId);
                        System.out.println("Using cached correlation for " + alphaId + ": " + productCorr);
                    }

                    mapProductCorrByAlphaId.put(alphaId, productCorr);

                    System.out.println("✔ DONE alpha " + alphaId
                            + " | corr = " + productCorr);

                    // 3️⃣ set favorite if correlation >= 0.7
                    if (productCorr != null && productCorr >= ConfigLoader.getMinCorrelation()) {
                        setFavoriteForAlpha(alphaId);
                        progressFinal.markFavorited(alphaId);
                    }

                    progressFinal.markCompleted(alphaId);

                } catch (Exception e) {
                    System.err.println("❌ ERROR alpha " + alphaId);
                    mapProductCorrByAlphaId.put(alphaId, null);
                    progressFinal.markFailed(alphaId, e.getMessage());
                    e.printStackTrace();
                }
            });

            futures.add(future);
        }

        // Wait for all tasks to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Shutdown thread pool
        executor.shutdown();

        // Print progress summary
        progress.printProgress();

        // Print correlation results (only alphas with corr < 0.7)
        double minCorrelation = ConfigLoader.getMinCorrelation();
        System.out.println("--------------- PRODUCT CORR RESULT (corr < " + minCorrelation + ") ----------------");

        StringBuilder result = new StringBuilder();

        mapProductCorrByAlphaId.entrySet().stream()
                .filter(e -> e.getValue() == null || e.getValue() < minCorrelation)
                .sorted(Map.Entry.comparingByValue(
                        Comparator.nullsFirst(Double::compareTo)
                ))
                .forEach(e -> {
                    result.append("Alpha: " + e.getKey() + ", Product corr = " + e.getValue() + "\n");
                    System.out.println("Alpha: " + e.getKey() + ", Product corr = " + e.getValue());
                });

        if (result.isEmpty()) {
            System.out.println("No alphas with correlation < " + minCorrelation);
        }

        sendResultByMail(result.toString());

        // Auto-submission disabled - user can submit manually via Dashboard UI
        // submitPendingAlphas(progress, mapProductCorrByAlphaId);
    }

    private static void submitPendingAlphas(ProgressTracker progress, Map<String, Double> mapProductCorrByAlphaId)
            throws IOException, InterruptedException {
        // Get alphas that need submission
        List<String> alphasToSubmit = progress.getAlphasToSubmit();

        if (alphasToSubmit.isEmpty()) {
            System.out.println("No alphas pending submission.");
            return;
        }

        double minCorrelation = ConfigLoader.getMinCorrelation();

        // Filter: Only submit alphas with correlation < 0.7 (or configured threshold)
        // Skip alphas with correlation >= 0.7
        List<String> filteredAlphas = alphasToSubmit.stream()
                .filter(id -> {
                    Double corr = mapProductCorrByAlphaId.get(id);
                    if (corr == null) {
                        return true; // Submit if correlation unknown
                    }
                    if (corr >= minCorrelation) {
                        System.out.println("Skipping submission for " + id + " (corr=" + corr + " >= " + minCorrelation + ")");
                        progress.markSkipped(id, "Correlation >= " + minCorrelation);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (filteredAlphas.isEmpty()) {
            System.out.println("No alphas to submit (all have correlation >= " + minCorrelation + ")");
            return;
        }

        System.out.println("--------------- SUBMITTING ALPHAS ----------------");
        System.out.println("Alphas to submit (corr < " + minCorrelation + "): " + filteredAlphas.size());

        // Sort by correlation (ascending)
        List<String> sortedAlphaIds = filteredAlphas.stream()
                .sorted(Comparator.comparing(
                        id -> mapProductCorrByAlphaId.getOrDefault(id, Double.MAX_VALUE)
                ))
                .collect(Collectors.toList());

        for (String alphaId : sortedAlphaIds) {
            boolean success = submitAlpha(alphaId);
            progress.markSubmitted(alphaId, success);
        }
    }
}
