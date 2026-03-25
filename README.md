# WorldQuant Brain Tool

A Java desktop application for managing and processing trading alphas on the WorldQuant Brain platform.

## Features

- **Desktop App (JavaFX)** - Native UI with tabs for session, config, filters, jobs, logs, and results
- **Regular Alpha Processing** - Process regular alphas with configurable filters
- **Super Alpha Processing** - Process super alphas with correlation monitoring
- **Mark Failed Alphas** - Auto-mark alphas with FAIL checks as favorite for easy filtering
- **Session Validation** - Validate session before processing
- **Resume Capability** - Resume from crashes with progress tracking
- **Quartz Scheduling** - Schedule jobs with cron expressions or intervals

## Requirements

- Java 17+
- Maven 3.6+

---

## Build & Run (after `git pull`)

### Step 1 — First time only: create your config file

```bash
cp config.properties.example config.properties
```

Then open `config.properties` and fill in your settings (cookie, email, filters).

### Step 2 — Build the JAR

> **macOS iCloud note:** If your project folder is inside `~/Documents` or `~/Desktop` (iCloud-synced), build from `/tmp` to avoid file-write timeouts:

```bash
# Recommended (avoids iCloud sync issues on macOS)
rsync -a --exclude='target/' /path/to/worldquant-brain-tool/ /tmp/wq-build/
cd /tmp/wq-build
mvn package -q
cp target/worldquant-brain-tool-1.0-SNAPSHOT-desktop.jar /path/to/worldquant-brain-tool/
```

> If your project is **not** in an iCloud-synced folder, you can build directly:

```bash
cd /path/to/worldquant-brain-tool
mvn package -q
```

The fat JAR will be at:
```
target/worldquant-brain-tool-1.0-SNAPSHOT-desktop.jar
```

### Step 3 — Run the desktop app

Place `config.properties` in the **same directory** as the JAR, then run:

```bash
java -jar worldquant-brain-tool-1.0-SNAPSHOT-desktop.jar
```

> Make sure Java 17+ is installed: `java -version`

---

## Quick Start

### 1. Setup Configuration

```bash
# Copy the example config
cp config.properties.example config.properties

# Edit config.properties with your settings
```

### 2. Configure Your Cookie

Get your WorldQuant Brain cookie:
1. Login to [platform.worldquantbrain.com](https://platform.worldquantbrain.com)
2. Open browser DevTools (F12) → Network tab
3. Copy the `Cookie` header from any request
4. Paste it in `config.properties` under `wq.cookie=` or update it in the **Session** tab of the desktop app

### 3. Build the Project

```bash
mvn package -q
```

## Running the Application

### Option 1: Desktop App (Recommended)

```bash
java -jar worldquant-brain-tool-1.0-SNAPSHOT-desktop.jar
```

Tabs available:
- **Session** — View/update cookie, validate session
- **Config** — General settings and Email/SMTP settings
- **Filters** — Regular & super alpha filter settings
- **Jobs** — Run jobs manually, mark failed alphas, view job history
- **Logs** — Real-time log viewer
- **Results** — View progress JSON files

### Option 2: Run Jobs Directly

**Regular Alpha Processing:**
```bash
mvn exec:java -Dexec.mainClass="demo.webapp.regular.RegularAlphaUtils"
```

**Super Alpha Processing:**
```bash
mvn exec:java -Dexec.mainClass="demo.webapp.regular.SuperAlphaUtils"
```

**Regular Alpha for Gen Super:**
```bash
mvn exec:java -Dexec.mainClass="demo.webapp.regular.RegularAlphaForGenSuperAlphaUtils"
```

**Clear progress and start fresh:**
```bash
mvn exec:java -Dexec.mainClass="demo.webapp.regular.RegularAlphaUtils" -Dexec.args="--clear"
```

### Option 3: Scheduled Jobs (Quartz)

**Start scheduler with config from properties:**
```bash
mvn exec:java -Dexec.mainClass="demo.webapp.scheduler.SchedulerApp"
```

**Interactive mode:**
```bash
mvn exec:java -Dexec.mainClass="demo.webapp.scheduler.SchedulerApp" -Dexec.args="--interactive"
```

**Command line scheduling:**
```bash
# Run Regular every 60 minutes
mvn exec:java -Dexec.mainClass="demo.webapp.scheduler.SchedulerApp" -Dexec.args="--regular 60"

# Run Super at 8 AM daily
mvn exec:java -Dexec.mainClass="demo.webapp.scheduler.SchedulerApp" -Dexec.args='--super "0 0 8 * * ?"'

# Multiple jobs
mvn exec:java -Dexec.mainClass="demo.webapp.scheduler.SchedulerApp" -Dexec.args="--regular 60 --super 120"
```

### Option 4: Session Validation Only

```bash
mvn exec:java -Dexec.mainClass="demo.webapp.SessionValidator"
```

## Configuration

### config.properties

```properties
# WorldQuant Brain API Cookie (required)
wq.cookie=YOUR_COOKIE_HERE

# Email Configuration
smtp.host=smtp.gmail.com
smtp.port=587
smtp.username=your-email@gmail.com
smtp.password=your-app-password
email.recipient=recipient@example.com

# Thread Pool
thread.pool.size=3

# Alpha Filter Configuration
alpha.min.correlation=0.7
alpha.min.fitness=1.3

# Regular Alpha Filters (configurable via web)
filter.regular.region=JPN
filter.regular.date.from=2026-01-29T00:00:00-05:00
filter.regular.date.to=2026-02-07T00:00:00-05:00
filter.regular.min.fitness=1.0
filter.regular.limit=5

# Scheduler (optional)
scheduler.regular.cron=0 0 8 * * ?
scheduler.super.interval.minutes=120
```

### Environment Variables

Override any config with environment variables:

```bash
export WQ_COOKIE="your-cookie-here"
export WQ_SMTP_PASSWORD="your-password"
export WQ_THREAD_POOL_SIZE=5
```

## Cron Expression Examples

| Expression | Description |
|------------|-------------|
| `0 0 8 * * ?` | Every day at 8:00 AM |
| `0 0 8,20 * * ?` | Every day at 8 AM and 8 PM |
| `0 0/30 * * * ?` | Every 30 minutes |
| `0 0 * * * ?` | Every hour |
| `0 0 8 ? * MON-FRI` | Weekdays at 8:00 AM |

## Project Structure

```
src/main/java/demo/webapp/
├── ConfigLoader.java          # Configuration management
├── Constant.java              # Constants
├── ProgressTracker.java       # Resume capability
├── SessionValidator.java      # Session validation
├── regular/
│   ├── RegularAlphaUtils.java
│   ├── SuperAlphaUtils.java
│   ├── RegularAlphaForGenSuperAlphaUtils.java
│   ├── MarkFailedAlphasUtils.java  # Mark FAIL-check alphas as favorite
│   └── EmailSender.java
├── scheduler/
│   ├── SchedulerApp.java      # Scheduler entry point
│   ├── SchedulerManager.java  # Quartz configuration
│   ├── RegularAlphaJob.java
│   ├── SuperAlphaJob.java
│   └── RegularAlphaForGenSuperJob.java
├── desktop/
│   ├── DesktopApp.java        # JavaFX application
│   ├── DesktopLauncher.java   # JAR entry point
│   ├── AppState.java          # Shared state & job runner
│   └── tabs/
│       ├── SessionTab.java
│       ├── ConfigTab.java
│       ├── FiltersTab.java
│       ├── JobsTab.java
│       ├── LogsTab.java
│       ├── ResultsTab.java
│       └── UiHelper.java
└── web/
    ├── WebServer.java         # HTTP server (legacy)
    ├── ApiHandler.java        # REST API endpoints
    └── StaticFileHandler.java # Web dashboard UI
```

## Progress Files

Progress is saved to JSON files for resume capability:

- `progress_regular.json` - Regular alpha progress
- `progress_super.json` - Super alpha progress
- `progress_regular_gen_super.json` - Gen super progress

These files are automatically created and updated during processing.

## API Endpoints (Web Dashboard)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/session` | GET | Get session status |
| `/api/session` | POST | Update cookie |
| `/api/filters` | GET | Get filter settings |
| `/api/filters` | POST | Update filters |
| `/api/jobs` | GET | List running jobs |
| `/api/run` | POST | Trigger manual run |
| `/api/results` | GET | Get historical results |

## Troubleshooting

### Session Invalid
1. Login to WorldQuant Brain in your browser
2. Copy the fresh cookie from DevTools
3. Update via web dashboard or config.properties

### Rate Limited (429)
- Reduce `thread.pool.size` to 2-3
- Add delays between requests

### Job Not Starting
- Check session validity first
- Verify filter settings return results
- Check console logs for errors

## License

MIT
