package demo.webapp.regular;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.webapp.ConfigLoader;
import demo.webapp.SessionValidator;
import demo.webapp.regular.entity.alphaIdResponse.AlphaItem;
import demo.webapp.regular.entity.alphaIdResponse.AlphaListResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static demo.webapp.Constant.COOKIE;

public class MarkFailedAlphasUtils {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ALPHAS = 1000;

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            SessionValidator.validateOrThrow();
        } catch (SessionValidator.SessionInvalidException e) {
            System.err.println("Cannot proceed: " + e.getMessage());
            return;
        }

        System.out.println("========== START MARK NOT-PASS ALPHAS AS FAVORITE ==========");

        List<AlphaItem> allAlphas = fetchAllAlphas();

        if (allAlphas == null || allAlphas.isEmpty()) {
            System.out.println("No alphas found with the current filter.");
            return;
        }

        System.out.println("Total alphas fetched: " + allAlphas.size());

        ObjectMapper mapper = new ObjectMapper();
        int marked = 0;
        int passed = 0;
        int skipped = 0;

        for (AlphaItem alpha : allAlphas) {
            String alphaId = alpha.getId();
            String isJson = alpha.getIs();

            if (isJson == null || isJson.isBlank() || isJson.equals("null")) {
                System.out.println("Alpha " + alphaId + " has no IS data, skipping.");
                skipped++;
                continue;
            }

            if (hasFailCheck(mapper, isJson)) {
                System.out.println("Alpha " + alphaId + " has FAIL check -> marking as FAVORITE");
                try {
                    RegularAlphaUtils.setFavoriteForAlpha(alphaId);
                    marked++;
                } catch (Exception e) {
                    System.err.println("Failed to set favorite for " + alphaId + ": " + e.getMessage());
                    skipped++;
                }
            } else {
                System.out.println("Alpha " + alphaId + " passes all checks -> skip");
                passed++;
            }
        }

        System.out.println("========== DONE ==========");
        System.out.println("Total processed : " + allAlphas.size());
        System.out.println("Marked favorite  : " + marked);
        System.out.println("Passed (skipped) : " + passed);
        System.out.println("Errors/no-data   : " + skipped);
    }

    /**
     * Returns true if the alpha's IS checks contain at least one result == "FAIL".
     * An alpha "passes" when NO check has result FAIL.
     */
    private static boolean hasFailCheck(ObjectMapper mapper, String isJson) {
        try {
            JsonNode isNode = mapper.readTree(isJson);
            JsonNode checks = isNode.get("checks");
            if (checks == null || !checks.isArray()) return false;
            for (JsonNode check : checks) {
                JsonNode result = check.get("result");
                if (result != null && "FAIL".equalsIgnoreCase(result.asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error parsing IS checks: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches up to MAX_ALPHAS alpha objects from the API using the saved filter settings.
     */
    private static List<AlphaItem> fetchAllAlphas() throws IOException, InterruptedException {
        String region    = ConfigLoader.getRegularFilterRegion();
        String dateFrom  = ConfigLoader.getRegularFilterDateFrom();
        String dateTo    = ConfigLoader.getRegularFilterDateTo();
        double minFitness = ConfigLoader.getRegularFilterMinFitness();

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        List<AlphaItem> all = new ArrayList<>();
        int offset = 0;

        System.out.println("Fetching alphas with filter: region=" + region
                + ", dateFrom=" + dateFrom + ", dateTo=" + dateTo
                + ", minFitness=" + minFitness);

        while (all.size() < MAX_ALPHAS) {
            int remaining = MAX_ALPHAS - all.size();
            int limit = Math.min(BATCH_SIZE, remaining);

            String url = "https://api.worldquantbrain.com/users/self/alphas"
                    + "?limit=" + limit
                    + "&offset=" + offset
                    + "&status=UNSUBMITTED%1FIS_FAIL"
                    + "&dateCreated%3E=" + dateFrom
                    + "&dateCreated%3C" + dateTo
                    + "&type=REGULAR"
                    + "&settings.region=" + region
                    + "&is.fitness%3E=" + minFitness
                    + "&order=-is.fitness"
                    + "&hidden=false";

            System.out.println("Fetching page offset=" + offset + " limit=" + limit);

            HttpResponse<String> response = sendGet(client, url);
            if (response == null) break;

            int status = response.statusCode();
            if (status != 200) {
                System.err.println("Unexpected status " + status + " fetching alphas, stopping.");
                break;
            }

            AlphaListResponse page = mapper.readValue(response.body(), AlphaListResponse.class);
            List<AlphaItem> results = page.getResults();

            if (results == null || results.isEmpty()) {
                System.out.println("No more results.");
                break;
            }

            all.addAll(results);
            System.out.println("Fetched " + results.size() + " alphas (total so far: " + all.size() + ")");

            // No more pages
            if (results.size() < limit) break;

            offset += limit;
        }

        return all;
    }

    private static HttpResponse<String> sendGet(HttpClient client, String url)
            throws IOException, InterruptedException {
        int maxRetry = 5;
        int waitMs = 2000;

        for (int retry = 0; retry < maxRetry; retry++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .header("Referer", "https://platform.worldquantbrain.com/")
                    .header("Cookie", COOKIE)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                System.out.println("Rate limited. Waiting " + waitMs + "ms...");
                Thread.sleep(waitMs);
                waitMs *= 2;
                continue;
            }

            return response;
        }

        System.err.println("Failed to fetch after max retries.");
        return null;
    }
}
