package demo.webapp;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global cooperative stop flag for long-running jobs.
 * Set by the desktop "Stop" button; checked by job worker loops.
 * Reset automatically when a new job starts.
 */
public class JobControl {

    private static final AtomicBoolean stopRequested = new AtomicBoolean(false);

    public static void requestStop() {
        stopRequested.set(true);
    }

    public static void reset() {
        stopRequested.set(false);
    }

    public static boolean isStopRequested() {
        return stopRequested.get();
    }
}
