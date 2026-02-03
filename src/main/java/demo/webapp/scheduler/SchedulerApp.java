package demo.webapp.scheduler;

import demo.webapp.ConfigLoader;
import demo.webapp.SessionValidator;
import org.quartz.SchedulerException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * Main entry point for the WorldQuant Brain Scheduler.
 *
 * Usage:
 *   java SchedulerApp                    - Start with config from properties
 *   java SchedulerApp --interactive      - Start with interactive console
 *   java SchedulerApp --regular 60       - Run Regular Alpha every 60 minutes
 *   java SchedulerApp --super "0 0 8 * * ?" - Run Super Alpha with cron
 *
 * Cron Expression Examples:
 *   "0 0 8 * * ?"      - Every day at 8:00 AM
 *   "0 0 8,20 * * ?"   - Every day at 8:00 AM and 8:00 PM
 *   "0 0/30 * * * ?"   - Every 30 minutes
 *   "0 0 * * * ?"      - Every hour
 *   "0 0 8 ? * MON-FRI" - Weekdays at 8:00 AM
 */
public class SchedulerApp {

    private static SchedulerManager schedulerManager;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  WorldQuant Brain Scheduler");
        System.out.println("========================================");

        // Validate session at startup
        System.out.println("\nValidating session...");
        SessionValidator.ValidationResult validation = SessionValidator.validate();
        if (!validation.isValid()) {
            System.err.println("WARNING: Session is invalid - " + validation.getMessage());
            System.err.println("Jobs will skip execution until session is updated.");
        } else {
            System.out.println("Session valid: " + validation.getUsername());
        }

        try {
            schedulerManager = new SchedulerManager();

            if (args.length == 0) {
                // Start with config from properties
                startFromConfig();
            } else if (args[0].equals("--interactive")) {
                // Interactive mode
                startInteractive();
            } else {
                // Parse command line arguments
                parseArgs(args);
            }

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down scheduler...");
                try {
                    if (schedulerManager != null) {
                        schedulerManager.shutdown(true);
                    }
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
            }));

            // Keep the application running
            if (!hasScheduledJobs()) {
                System.out.println("\nNo jobs scheduled. Use --interactive for manual configuration.");
                return;
            }

            System.out.println("\nScheduler running. Press Ctrl+C to stop.");
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Scheduler error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startFromConfig() throws SchedulerException {
        System.out.println("\nLoading schedule configuration from config.properties...");

        schedulerManager.start();
        schedulerManager.configureFromProperties();
        schedulerManager.listJobs();
    }

    private static void parseArgs(String[] args) throws SchedulerException {
        schedulerManager.start();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--regular" -> {
                    if (i + 1 < args.length) {
                        String schedule = args[++i];
                        if (schedule.contains(" ")) {
                            // Cron expression
                            schedulerManager.scheduleRegularAlphaJob(schedule);
                        } else {
                            // Interval in minutes
                            int interval = Integer.parseInt(schedule);
                            schedulerManager.scheduleJobWithInterval(
                                    RegularAlphaJob.class, "regularAlphaJob", interval);
                        }
                    }
                }
                case "--super" -> {
                    if (i + 1 < args.length) {
                        String schedule = args[++i];
                        if (schedule.contains(" ")) {
                            schedulerManager.scheduleSuperAlphaJob(schedule);
                        } else {
                            int interval = Integer.parseInt(schedule);
                            schedulerManager.scheduleJobWithInterval(
                                    SuperAlphaJob.class, "superAlphaJob", interval);
                        }
                    }
                }
                case "--gen-super" -> {
                    if (i + 1 < args.length) {
                        String schedule = args[++i];
                        if (schedule.contains(" ")) {
                            schedulerManager.scheduleRegularAlphaForGenSuperJob(schedule);
                        } else {
                            int interval = Integer.parseInt(schedule);
                            schedulerManager.scheduleJobWithInterval(
                                    RegularAlphaForGenSuperJob.class, "regularAlphaForGenSuperJob", interval);
                        }
                    }
                }
                case "--help" -> {
                    printHelp();
                    System.exit(0);
                }
            }
        }

        schedulerManager.listJobs();
    }

    private static void startInteractive() throws SchedulerException {
        schedulerManager.start();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printInteractiveHelp();

        while (running) {
            System.out.print("\nscheduler> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+", 3);
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "help" -> printInteractiveHelp();

                    case "list" -> schedulerManager.listJobs();

                    case "schedule" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: schedule <job> <cron|interval>");
                            continue;
                        }
                        scheduleJob(parts[1], parts[2]);
                    }

                    case "run" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: run <job>");
                            continue;
                        }
                        runJobNow(parts[1]);
                    }

                    case "pause" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: pause <job>");
                            continue;
                        }
                        schedulerManager.pauseJob(getJobName(parts[1]));
                    }

                    case "resume" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: resume <job>");
                            continue;
                        }
                        schedulerManager.resumeJob(getJobName(parts[1]));
                    }

                    case "remove" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: remove <job>");
                            continue;
                        }
                        schedulerManager.removeJob(getJobName(parts[1]));
                    }

                    case "status" -> {
                        SessionValidator.ValidationResult status = SessionValidator.validate();
                        System.out.println("Session: " + (status.isValid() ? "Valid" : "Invalid"));
                    }

                    case "exit", "quit" -> {
                        running = false;
                        schedulerManager.shutdown();
                    }

                    default -> System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void scheduleJob(String jobType, String schedule) throws SchedulerException {
        boolean isCron = schedule.contains(" ");

        switch (jobType.toLowerCase()) {
            case "regular" -> {
                if (isCron) {
                    schedulerManager.scheduleRegularAlphaJob(schedule);
                } else {
                    int interval = Integer.parseInt(schedule);
                    schedulerManager.scheduleJobWithInterval(
                            RegularAlphaJob.class, "regularAlphaJob", interval);
                }
            }
            case "super" -> {
                if (isCron) {
                    schedulerManager.scheduleSuperAlphaJob(schedule);
                } else {
                    int interval = Integer.parseInt(schedule);
                    schedulerManager.scheduleJobWithInterval(
                            SuperAlphaJob.class, "superAlphaJob", interval);
                }
            }
            case "gen-super", "gensuper" -> {
                if (isCron) {
                    schedulerManager.scheduleRegularAlphaForGenSuperJob(schedule);
                } else {
                    int interval = Integer.parseInt(schedule);
                    schedulerManager.scheduleJobWithInterval(
                            RegularAlphaForGenSuperJob.class, "regularAlphaForGenSuperJob", interval);
                }
            }
            default -> System.out.println("Unknown job type: " + jobType);
        }
    }

    private static void runJobNow(String jobType) throws SchedulerException {
        String jobName = getJobName(jobType);

        // Check if job exists, if not create a one-time job
        try {
            schedulerManager.triggerJobNow(jobName);
        } catch (Exception e) {
            // Job doesn't exist, run directly
            System.out.println("Running " + jobType + " job immediately...");
            try {
                switch (jobType.toLowerCase()) {
                    case "regular" -> new RegularAlphaJob().execute(null);
                    case "super" -> new SuperAlphaJob().execute(null);
                    case "gen-super", "gensuper" -> new RegularAlphaForGenSuperJob().execute(null);
                    default -> System.out.println("Unknown job type: " + jobType);
                }
            } catch (Exception ex) {
                System.err.println("Job execution failed: " + ex.getMessage());
            }
        }
    }

    private static String getJobName(String jobType) {
        return switch (jobType.toLowerCase()) {
            case "regular" -> "regularAlphaJob";
            case "super" -> "superAlphaJob";
            case "gen-super", "gensuper" -> "regularAlphaForGenSuperJob";
            default -> jobType; // Assume it's already a job name
        };
    }

    private static boolean hasScheduledJobs() throws SchedulerException {
        return !schedulerManager.getScheduler().getJobGroupNames().isEmpty() &&
               !schedulerManager.getScheduler().getJobKeys(
                       org.quartz.impl.matchers.GroupMatcher.anyGroup()).isEmpty();
    }

    private static void printHelp() {
        System.out.println("""

            WorldQuant Brain Scheduler - Usage:

            java -cp ... demo.webapp.scheduler.SchedulerApp [options]

            Options:
              (no args)              Load schedule from config.properties
              --interactive          Start interactive console mode
              --regular <schedule>   Schedule Regular Alpha job
              --super <schedule>     Schedule Super Alpha job
              --gen-super <schedule> Schedule Regular Alpha for Gen Super job
              --help                 Show this help message

            Schedule can be:
              - A number (interval in minutes): 60
              - A cron expression: "0 0 8 * * ?"

            Cron Expression Format:
              sec min hour day month weekday [year]

            Examples:
              "0 0 8 * * ?"        Every day at 8:00 AM
              "0 0 8,20 * * ?"     Every day at 8 AM and 8 PM
              "0 0/30 * * * ?"     Every 30 minutes
              "0 0 * * * ?"        Every hour on the hour
              "0 0 8 ? * MON-FRI"  Weekdays at 8:00 AM

            Config Properties (config.properties):
              scheduler.regular.cron=0 0 8 * * ?
              scheduler.regular.interval.minutes=60
              scheduler.super.cron=0 0 20 * * ?
              scheduler.regular_gen_super.interval.minutes=120
            """);
    }

    private static void printInteractiveHelp() {
        System.out.println("""

            Interactive Commands:
              schedule <job> <cron|minutes>  - Schedule a job
              run <job>                      - Run job immediately
              list                           - List all scheduled jobs
              pause <job>                    - Pause a job
              resume <job>                   - Resume a paused job
              remove <job>                   - Remove a scheduled job
              status                         - Check session status
              help                           - Show this help
              exit                           - Exit scheduler

            Job types: regular, super, gen-super

            Examples:
              schedule regular 60            - Run Regular every 60 min
              schedule super "0 0 8 * * ?"   - Run Super at 8 AM daily
              run regular                    - Run Regular now
            """);
    }
}
