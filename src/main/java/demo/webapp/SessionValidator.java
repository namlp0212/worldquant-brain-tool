package demo.webapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Validates the WorldQuant Brain session before starting batch operations.
 * This prevents wasting time on a full run with an expired session.
 */
public class SessionValidator {

    private static final String USER_SELF_URL = "https://api.worldquantbrain.com/users/self";
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Result of session validation containing status and user info.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String userId;
        private final String email;
        private final String username;

        private ValidationResult(boolean valid, String message, String userId, String email, String username) {
            this.valid = valid;
            this.message = message;
            this.userId = userId;
            this.email = email;
            this.username = username;
        }

        public static ValidationResult success(String userId, String email, String username) {
            return new ValidationResult(true, "Session is valid", userId, email, username);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null, null, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        @Override
        public String toString() {
            if (valid) {
                return String.format("Session valid - User: %s (%s)", username, email);
            } else {
                return String.format("Session invalid - %s", message);
            }
        }
    }

    /**
     * Validates the current session by calling the /users/self endpoint.
     *
     * @return ValidationResult containing status and user info if valid
     */
    public static ValidationResult validate() {
        System.out.println("========== SESSION VALIDATION ==========");

        String cookie = ConfigLoader.getCookie();

        if (cookie == null || cookie.isBlank()) {
            System.err.println("ERROR: Cookie is not configured!");
            System.err.println("Please set wq.cookie in config.properties or WQ_COOKIE environment variable");
            return ValidationResult.failure("Cookie is not configured");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_SELF_URL))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .header("Referer", "https://platform.worldquantbrain.com/")
                .header("Cookie", cookie)
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            System.out.println("Validation request status: " + status);

            // Session is valid
            if (status == 200) {
                return parseUserResponse(body);
            }

            // Unauthorized - session expired or invalid
            if (status == 401 || status == 403) {
                System.err.println("ERROR: Session is expired or invalid!");
                System.err.println("Please update your cookie in config.properties");
                System.err.println("Response: " + body);
                return ValidationResult.failure("Session expired or unauthorized (HTTP " + status + ")");
            }

            // Rate limited
            if (status == 429) {
                System.err.println("WARNING: Rate limited during validation. Session might be valid.");
                return ValidationResult.failure("Rate limited during validation (HTTP 429)");
            }

            // Other errors
            System.err.println("ERROR: Unexpected response during validation");
            System.err.println("Status: " + status);
            System.err.println("Response: " + body);
            return ValidationResult.failure("Unexpected error (HTTP " + status + ")");

        } catch (IOException e) {
            System.err.println("ERROR: Network error during session validation");
            System.err.println("Message: " + e.getMessage());
            return ValidationResult.failure("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ERROR: Session validation interrupted");
            return ValidationResult.failure("Validation interrupted");
        }
    }

    /**
     * Validates session and throws exception if invalid.
     * Use this for fail-fast behavior at the start of batch operations.
     *
     * @throws SessionInvalidException if session is not valid
     */
    public static ValidationResult validateOrThrow() throws SessionInvalidException {
        ValidationResult result = validate();
        if (!result.isValid()) {
            throw new SessionInvalidException(result.getMessage());
        }
        System.out.println("Session validated successfully: " + result);
        System.out.println("=========================================");
        return result;
    }

    /**
     * Quick check if session is valid without detailed info.
     *
     * @return true if session is valid, false otherwise
     */
    public static boolean isSessionValid() {
        return validate().isValid();
    }

    private static ValidationResult parseUserResponse(String body) {
        try {
            JsonNode root = mapper.readTree(body);

            String userId = getJsonString(root, "id");
            String email = getJsonString(root, "email");
            String username = getJsonString(root, "username");

            // Also extract some useful info for logging
            String credential = getJsonString(root, "credential");
            JsonNode consultantNode = root.get("consultant");
            String consultantStatus = consultantNode != null ? getJsonString(consultantNode, "status") : "N/A";

            System.out.println("User ID: " + userId);
            System.out.println("Email: " + email);
            System.out.println("Username: " + username);
            System.out.println("Credential: " + credential);
            System.out.println("Consultant Status: " + consultantStatus);

            return ValidationResult.success(userId, email, username);

        } catch (Exception e) {
            System.err.println("WARNING: Could not parse user response, but session appears valid");
            System.err.println("Parse error: " + e.getMessage());
            return ValidationResult.success("unknown", "unknown", "unknown");
        }
    }

    private static String getJsonString(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        return "N/A";
    }

    /**
     * Exception thrown when session validation fails.
     */
    public static class SessionInvalidException extends Exception {
        public SessionInvalidException(String message) {
            super(message);
        }
    }

    /**
     * Main method for standalone session validation testing.
     */
    public static void main(String[] args) {
        try {
            ValidationResult result = validateOrThrow();
            System.out.println("\nSession is valid and ready to use!");
            System.out.println(result);
        } catch (SessionInvalidException e) {
            System.err.println("\nSession validation failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
