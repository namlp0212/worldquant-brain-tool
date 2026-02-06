package demo.webapp.regular;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.webapp.regular.entity.alphaIdResponse.AlphaItem;
import demo.webapp.regular.entity.alphaIdResponse.AlphaListResponse;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import demo.webapp.ConfigLoader;
import demo.webapp.ProgressTracker;
import demo.webapp.SessionValidator;

import static demo.webapp.Constant.COOKIE;

public class RegularAlphaForGenSuperAlphaUtils {

    public static Double getProdCorrOfAlpha(String alphaId) throws InterruptedException, IOException {
        System.out.println("----- START CHECK PRODUCT CORR OF ALPHA " + alphaId + " ------");

        String url = "https://api.worldquantbrain.com/alphas/"
                + alphaId + "/correlations/prod";

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        int maxRetry = 1000;
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

        String url = "https://api.worldquantbrain.com/users/self/alphas" +
                "?limit=100" +
                "&offset=100" +
                "&status!=UNSUBMITTED%1FIS-FAIL" +
                "&settings.region=IND" +
                "&type=REGULAR" +
                "&order=is.prodCorrelation" +
                "&hidden=false";

        HttpClient client = HttpClient.newHttpClient();

        int maxRetry = 5;
        int retry = 0;
        int waitMs = 2000; // 2 giây

        while (retry < maxRetry) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Referer", "https://platform.worldquantbrain.com/")
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
        String url = "https://api.worldquantbrain.com/alphas/" + alphaId + "/submit";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .header("Origin", "https://platform.worldquantbrain.com")
                .header("Referer", "https://platform.worldquantbrain.com/")
                .header("Cookie", COOKIE)
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status == 200) {
            System.out.println("✔ SUBMIT ALPHA " + alphaId + " SUCCESS ");
            System.out.println("Body: " + response.body());
            return true;
        } else {
            System.out.println("❌ SUBMIT ALPHA " + alphaId + " FAIL ");
            System.out.println("Body: " + response.body());
            return false;
        }

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
        ProgressTracker progress = new ProgressTracker("REGULAR_GEN_SUPER");

        if (clearProgress) {
            System.out.println("Clearing previous progress...");
            progress.clear();
            progress = new ProgressTracker("REGULAR_GEN_SUPER");
        }

        if (progress.hasExistingProgress()) {
            System.out.println("Resuming from previous progress...");
            progress.printProgress();
        }

        ExecutorService executor = Executors.newFixedThreadPool(ConfigLoader.getThreadPoolSize());

        Map<String, Double> mapProductCorrByAlphaId = Collections.synchronizedMap(new HashMap<>());

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

                    mapProductCorrByAlphaId.put(alphaId, 1.0);
                    progressFinal.markCompleted(alphaId);

                    System.out.println("✔ DONE alpha " + alphaId);

                } catch (Exception e) {
                    System.err.println("❌ ERROR alpha " + alphaId);
                    mapProductCorrByAlphaId.put(alphaId, 0.0);
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

        // Print results
        System.out.println("--------------- RESULT ----------------");

        StringBuilder result = new StringBuilder();

        mapProductCorrByAlphaId.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                        Comparator.nullsFirst(Double::compareTo)
                ))
                .forEach(e -> {
                    if (Objects.equals(1.0, e.getValue())) {
                        result.append(e.getKey()).append("\n");
                    }
                });

        sendResultByMail(result.toString());
    }
}
