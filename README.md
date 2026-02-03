# WorldQuant Brain Tool

A Java automation tool for managing and processing trading alphas on the WorldQuant Brain platform.

## Features

- **Regular Alpha Processing** - Process regular alphas with configurable filters
- **Super Alpha Processing** - Process super alphas with correlation monitoring
- **Session Validation** - Validate session before processing
- **Resume Capability** - Resume from crashes with progress tracking
- **Quartz Scheduling** - Schedule jobs with cron expressions or intervals
- **Web Dashboard** - Monitor jobs, update settings, view results

## Requirements

- Java 17+
- Maven 3.6+

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
4. Paste it in `config.properties` under `wq.cookie=`

### 3. Build the Project

```bash
mvn clean compile
```

## Running the Application

### Option 1: Web Dashboard (Recommended)

Start the web server and manage everything from your browser:

```bash
mvn exec:java -Dexec.mainClass="demo.webapp.web.WebServer"
```

Open: **http://localhost:8080**

Features:
- View session status
- Update cookie (no restart needed)
- Configure filters (region, date range, fitness)
- Trigger manual job runs
- Monitor running jobs
- View historical results

Custom port:
```bash
mvn exec:java -Dexec.mainClass="demo.webapp.web.WebServer" -Dexec.args="3000"
```

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
├── Main.java                  # Main entry (email test)
├── ProgressTracker.java       # Resume capability
├── SessionValidator.java      # Session validation
├── regular/
│   ├── RegularAlphaUtils.java
│   ├── SuperAlphaUtils.java
│   ├── RegularAlphaForGenSuperAlphaUtils.java
│   └── EmailSender.java
├── scheduler/
│   ├── SchedulerApp.java      # Scheduler entry point
│   ├── SchedulerManager.java  # Quartz configuration
│   ├── RegularAlphaJob.java
│   ├── SuperAlphaJob.java
│   └── RegularAlphaForGenSuperJob.java
└── web/
    ├── WebServer.java         # HTTP server
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
