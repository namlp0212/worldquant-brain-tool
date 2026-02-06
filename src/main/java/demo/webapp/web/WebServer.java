package demo.webapp.web;

import com.sun.net.httpserver.HttpServer;
import demo.webapp.ConfigLoader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Simple web server for WorldQuant Brain Tool dashboard.
 *
 * Features:
 * - Change session/cookie value
 * - Monitor running jobs
 * - View historical results
 * - Trigger manual runs
 */
public class WebServer {

    private final HttpServer server;
    private final int port;

    public WebServer(int port) throws IOException {
        this.port = port;

        // Initialize log capture before anything else
        ApiHandler.LogsHandler.initLogCapture();

        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        // Set up thread pool
        server.setExecutor(Executors.newFixedThreadPool(10));

        // Register handlers
        setupHandlers();
    }

    private void setupHandlers() {
        // Static files (HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());

        // API endpoints
        server.createContext("/api/config", new ApiHandler.ConfigHandler());
        server.createContext("/api/session", new ApiHandler.SessionHandler());
        server.createContext("/api/jobs", new ApiHandler.JobsHandler());
        server.createContext("/api/results", new ApiHandler.ResultsHandler());
        server.createContext("/api/run", new ApiHandler.RunHandler());
        server.createContext("/api/progress", new ApiHandler.ProgressHandler());
        server.createContext("/api/filters", new ApiHandler.FiltersHandler());
        server.createContext("/api/submit", new ApiHandler.SubmitHandler());
        server.createContext("/api/logs", new ApiHandler.LogsHandler());
        server.createContext("/api/logs/stream", new ApiHandler.LogStreamHandler());
    }

    public void start() {
        server.start();
        System.out.println("========================================");
        System.out.println("  WorldQuant Brain Tool - Web Dashboard");
        System.out.println("========================================");
        System.out.println("Server started at: http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop");
        System.out.println("========================================");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Server stopped");
    }

    public static void main(String[] args) {
        int port = ConfigLoader.getInt("web.port", 8080);

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + port);
            }
        }

        try {
            WebServer webServer = new WebServer(port);
            webServer.start();

            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down web server...");
                webServer.stop();
            }));

            // Keep running
            Thread.currentThread().join();

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
