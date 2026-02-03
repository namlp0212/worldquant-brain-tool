package demo.webapp.scheduler;

import demo.webapp.ConfigLoader;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.Date;
import java.util.Properties;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Manages Quartz scheduler configuration and job scheduling.
 */
public class SchedulerManager {

    private final Scheduler scheduler;

    public SchedulerManager() throws SchedulerException {
        // Configure Quartz properties
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "WorldQuantBrainScheduler");
        props.setProperty("org.quartz.threadPool.threadCount", "3");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

        // Disable update check
        props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");

        StdSchedulerFactory factory = new StdSchedulerFactory(props);
        this.scheduler = factory.getScheduler();
    }

    /**
     * Starts the scheduler.
     */
    public void start() throws SchedulerException {
        scheduler.start();
        System.out.println("Scheduler started successfully");
    }

    /**
     * Shuts down the scheduler.
     */
    public void shutdown() throws SchedulerException {
        shutdown(true);
    }

    /**
     * Shuts down the scheduler.
     *
     * @param waitForJobsToComplete If true, waits for running jobs to complete
     */
    public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
        scheduler.shutdown(waitForJobsToComplete);
        System.out.println("Scheduler shut down");
    }

    /**
     * Schedules the Regular Alpha job with a cron expression.
     *
     * @param cronExpression Cron expression (e.g., "0 0 8 * * ?" for 8 AM daily)
     */
    public void scheduleRegularAlphaJob(String cronExpression) throws SchedulerException {
        JobDetail job = newJob(RegularAlphaJob.class)
                .withIdentity("regularAlphaJob", "alphaGroup")
                .withDescription("Process Regular Alphas")
                .build();

        CronTrigger trigger = newTrigger()
                .withIdentity("regularAlphaTrigger", "alphaGroup")
                .withSchedule(cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        scheduleJob(job, trigger, "Regular Alpha");
    }

    /**
     * Schedules the Super Alpha job with a cron expression.
     *
     * @param cronExpression Cron expression
     */
    public void scheduleSuperAlphaJob(String cronExpression) throws SchedulerException {
        JobDetail job = newJob(SuperAlphaJob.class)
                .withIdentity("superAlphaJob", "alphaGroup")
                .withDescription("Process Super Alphas")
                .build();

        CronTrigger trigger = newTrigger()
                .withIdentity("superAlphaTrigger", "alphaGroup")
                .withSchedule(cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        scheduleJob(job, trigger, "Super Alpha");
    }

    /**
     * Schedules the Regular Alpha for Gen Super job with a cron expression.
     *
     * @param cronExpression Cron expression
     */
    public void scheduleRegularAlphaForGenSuperJob(String cronExpression) throws SchedulerException {
        JobDetail job = newJob(RegularAlphaForGenSuperJob.class)
                .withIdentity("regularAlphaForGenSuperJob", "alphaGroup")
                .withDescription("Process Regular Alphas for Super Alpha Generation")
                .build();

        CronTrigger trigger = newTrigger()
                .withIdentity("regularAlphaForGenSuperTrigger", "alphaGroup")
                .withSchedule(cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        scheduleJob(job, trigger, "Regular Alpha for Gen Super");
    }

    /**
     * Schedules a job with interval-based trigger (in minutes).
     *
     * @param jobClass       The job class to schedule
     * @param jobName        Unique job name
     * @param intervalMinutes Interval in minutes between executions
     */
    public void scheduleJobWithInterval(Class<? extends Job> jobClass, String jobName, int intervalMinutes)
            throws SchedulerException {
        JobDetail job = newJob(jobClass)
                .withIdentity(jobName, "alphaGroup")
                .build();

        SimpleTrigger trigger = newTrigger()
                .withIdentity(jobName + "Trigger", "alphaGroup")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(intervalMinutes)
                        .repeatForever()
                        .withMisfireHandlingInstructionFireNow())
                .build();

        scheduleJob(job, trigger, jobName);
    }

    /**
     * Schedules a job with interval-based trigger (in hours).
     */
    public void scheduleJobWithIntervalHours(Class<? extends Job> jobClass, String jobName, int intervalHours)
            throws SchedulerException {
        JobDetail job = newJob(jobClass)
                .withIdentity(jobName, "alphaGroup")
                .build();

        SimpleTrigger trigger = newTrigger()
                .withIdentity(jobName + "Trigger", "alphaGroup")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInHours(intervalHours)
                        .repeatForever()
                        .withMisfireHandlingInstructionFireNow())
                .build();

        scheduleJob(job, trigger, jobName);
    }

    /**
     * Triggers a job to run immediately (one-time execution).
     */
    public void triggerJobNow(String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, "alphaGroup");
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey);
            System.out.println("Triggered job immediately: " + jobName);
        } else {
            System.err.println("Job not found: " + jobName);
        }
    }

    /**
     * Pauses a scheduled job.
     */
    public void pauseJob(String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, "alphaGroup");
        scheduler.pauseJob(jobKey);
        System.out.println("Paused job: " + jobName);
    }

    /**
     * Resumes a paused job.
     */
    public void resumeJob(String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, "alphaGroup");
        scheduler.resumeJob(jobKey);
        System.out.println("Resumed job: " + jobName);
    }

    /**
     * Removes a scheduled job.
     */
    public void removeJob(String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, "alphaGroup");
        scheduler.deleteJob(jobKey);
        System.out.println("Removed job: " + jobName);
    }

    /**
     * Lists all scheduled jobs.
     */
    public void listJobs() throws SchedulerException {
        System.out.println("\n========== SCHEDULED JOBS ==========");

        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                System.out.println("Job: " + jobKey.getName());
                System.out.println("  Description: " + jobDetail.getDescription());

                for (Trigger trigger : scheduler.getTriggersOfJob(jobKey)) {
                    System.out.println("  Trigger: " + trigger.getKey().getName());
                    System.out.println("  Next fire: " + trigger.getNextFireTime());

                    if (trigger instanceof CronTrigger) {
                        System.out.println("  Cron: " + ((CronTrigger) trigger).getCronExpression());
                    } else if (trigger instanceof SimpleTrigger) {
                        SimpleTrigger st = (SimpleTrigger) trigger;
                        System.out.println("  Interval: " + (st.getRepeatInterval() / 1000 / 60) + " minutes");
                    }
                }
                System.out.println();
            }
        }
        System.out.println("=====================================\n");
    }

    /**
     * Gets the underlying Quartz scheduler.
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    private void scheduleJob(JobDetail job, Trigger trigger, String description) throws SchedulerException {
        Date nextFire = scheduler.scheduleJob(job, trigger);
        System.out.println("Scheduled: " + description);
        System.out.println("  Next execution: " + nextFire);
    }

    /**
     * Configures jobs from config.properties.
     * Config keys:
     *   scheduler.regular.cron = cron expression
     *   scheduler.super.cron = cron expression
     *   scheduler.regular_gen_super.cron = cron expression
     *   scheduler.regular.interval.minutes = interval in minutes
     *   etc.
     */
    public void configureFromProperties() throws SchedulerException {
        // Regular Alpha Job
        String regularCron = ConfigLoader.get("scheduler.regular.cron");
        int regularInterval = ConfigLoader.getInt("scheduler.regular.interval.minutes", 0);

        if (regularCron != null && !regularCron.isBlank()) {
            scheduleRegularAlphaJob(regularCron);
        } else if (regularInterval > 0) {
            scheduleJobWithInterval(RegularAlphaJob.class, "regularAlphaJob", regularInterval);
        }

        // Super Alpha Job
        String superCron = ConfigLoader.get("scheduler.super.cron");
        int superInterval = ConfigLoader.getInt("scheduler.super.interval.minutes", 0);

        if (superCron != null && !superCron.isBlank()) {
            scheduleSuperAlphaJob(superCron);
        } else if (superInterval > 0) {
            scheduleJobWithInterval(SuperAlphaJob.class, "superAlphaJob", superInterval);
        }

        // Regular Alpha for Gen Super Job
        String genSuperCron = ConfigLoader.get("scheduler.regular_gen_super.cron");
        int genSuperInterval = ConfigLoader.getInt("scheduler.regular_gen_super.interval.minutes", 0);

        if (genSuperCron != null && !genSuperCron.isBlank()) {
            scheduleRegularAlphaForGenSuperJob(genSuperCron);
        } else if (genSuperInterval > 0) {
            scheduleJobWithInterval(RegularAlphaForGenSuperJob.class, "regularAlphaForGenSuperJob", genSuperInterval);
        }
    }
}
