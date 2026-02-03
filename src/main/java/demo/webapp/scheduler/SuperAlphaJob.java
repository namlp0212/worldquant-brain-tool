package demo.webapp.scheduler;

import demo.webapp.SessionValidator;
import demo.webapp.regular.SuperAlphaUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz Job for processing Super Alphas.
 */
public class SuperAlphaJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getJobDetail().getKey().getName();
        System.out.println("========================================");
        System.out.println("SCHEDULED JOB STARTED: " + jobName);
        System.out.println("Trigger: " + context.getTrigger().getKey().getName());
        System.out.println("Fire time: " + context.getFireTime());
        System.out.println("========================================");

        try {
            // Validate session first
            SessionValidator.ValidationResult validation = SessionValidator.validate();
            if (!validation.isValid()) {
                System.err.println("Session invalid, skipping job execution: " + validation.getMessage());
                notifySessionExpired(jobName, validation.getMessage());
                return;
            }

            // Run the main processing logic
            SuperAlphaUtils.main(new String[]{});

            System.out.println("========================================");
            System.out.println("SCHEDULED JOB COMPLETED: " + jobName);
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Job execution failed: " + e.getMessage());
            e.printStackTrace();
            throw new JobExecutionException("SuperAlphaJob failed", e);
        }
    }

    private void notifySessionExpired(String jobName, String message) {
        System.err.println("WARNING: Scheduled job '" + jobName + "' skipped due to invalid session.");
        System.err.println("Please update your cookie in config.properties");
    }
}
