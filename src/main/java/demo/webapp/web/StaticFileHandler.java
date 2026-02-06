package demo.webapp.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves static files and the embedded HTML dashboard.
 */
public class StaticFileHandler implements HttpHandler {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("ico", "image/x-icon");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Serve index.html for root path
        if (path.equals("/") || path.equals("/index.html")) {
            serveEmbeddedHtml(exchange);
            return;
        }

        // Try to serve from classpath
        String resourcePath = "/static" + path;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                byte[] content = is.readAllBytes();
                String ext = path.substring(path.lastIndexOf('.') + 1);
                String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
                return;
            }
        }

        // 404 Not Found
        String notFound = "Not Found: " + path;
        exchange.sendResponseHeaders(404, notFound.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(notFound.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void serveEmbeddedHtml(HttpExchange exchange) throws IOException {
        String html = getEmbeddedHtml();
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getEmbeddedHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WorldQuant Brain Tool - Dashboard</title>
    <style>
        :root {
            --primary: #2563eb;
            --primary-dark: #1d4ed8;
            --success: #16a34a;
            --danger: #dc2626;
            --warning: #d97706;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text: #1e293b;
            --text-muted: #64748b;
            --border: #e2e8f0;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: var(--bg);
            color: var(--text);
            line-height: 1.6;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }

        header {
            background: var(--primary);
            color: white;
            padding: 20px 0;
            margin-bottom: 30px;
        }

        header h1 {
            font-size: 1.5rem;
            font-weight: 600;
        }

        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }

        .card {
            background: var(--card-bg);
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            padding: 20px;
        }

        .card h2 {
            font-size: 1.1rem;
            margin-bottom: 15px;
            color: var(--text);
            border-bottom: 2px solid var(--primary);
            padding-bottom: 10px;
        }

        .status-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 500;
        }

        .status-valid { background: #dcfce7; color: var(--success); }
        .status-invalid { background: #fee2e2; color: var(--danger); }
        .status-running { background: #dbeafe; color: var(--primary); }
        .status-completed { background: #dcfce7; color: var(--success); }
        .status-failed { background: #fee2e2; color: var(--danger); }

        .btn {
            display: inline-block;
            padding: 10px 20px;
            border: none;
            border-radius: 6px;
            font-size: 0.9rem;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;
        }

        .btn-primary {
            background: var(--primary);
            color: white;
        }

        .btn-primary:hover {
            background: var(--primary-dark);
        }

        .btn-success {
            background: var(--success);
            color: white;
        }

        .btn-danger {
            background: var(--danger);
            color: white;
        }

        .btn-sm {
            padding: 6px 12px;
            font-size: 0.8rem;
        }

        .btn:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        input, textarea {
            width: 100%;
            padding: 10px;
            border: 1px solid var(--border);
            border-radius: 6px;
            font-size: 0.9rem;
            margin-bottom: 10px;
        }

        textarea {
            min-height: 100px;
            font-family: monospace;
        }

        .form-group {
            margin-bottom: 15px;
        }

        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: 500;
            color: var(--text-muted);
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            padding: 10px;
            text-align: left;
            border-bottom: 1px solid var(--border);
        }

        th {
            font-weight: 600;
            color: var(--text-muted);
            font-size: 0.85rem;
        }

        .job-actions {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            margin-top: 15px;
        }

        .info-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid var(--border);
        }

        .info-row:last-child {
            border-bottom: none;
        }

        .info-label {
            color: var(--text-muted);
        }

        .info-value {
            font-weight: 500;
        }

        .tab-container {
            margin-top: 20px;
        }

        .tabs {
            display: flex;
            gap: 5px;
            border-bottom: 2px solid var(--border);
            margin-bottom: 15px;
        }

        .tab {
            padding: 10px 20px;
            cursor: pointer;
            border: none;
            background: none;
            font-size: 0.9rem;
            color: var(--text-muted);
            border-bottom: 2px solid transparent;
            margin-bottom: -2px;
        }

        .tab.active {
            color: var(--primary);
            border-bottom-color: var(--primary);
        }

        .tab-content {
            display: none;
        }

        .tab-content.active {
            display: block;
        }

        .progress-list {
            max-height: 400px;
            overflow-y: auto;
        }

        .alpha-item {
            padding: 10px;
            border-bottom: 1px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .alpha-item:hover {
            background: var(--bg);
        }

        .correlation {
            font-family: monospace;
            font-weight: 600;
        }

        .corr-good { color: var(--success); }
        .corr-bad { color: var(--danger); }

        .loading {
            text-align: center;
            padding: 20px;
            color: var(--text-muted);
        }

        .toast {
            position: fixed;
            bottom: 20px;
            right: 20px;
            padding: 15px 25px;
            border-radius: 6px;
            color: white;
            font-weight: 500;
            z-index: 1000;
            animation: slideIn 0.3s ease;
        }

        .toast-success { background: var(--success); }
        .toast-error { background: var(--danger); }

        @keyframes slideIn {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }

        .refresh-btn {
            float: right;
            font-size: 0.8rem;
        }
    </style>
</head>
<body>
    <header>
        <div class="container">
            <h1>WorldQuant Brain Tool - Dashboard</h1>
        </div>
    </header>

    <div class="container">
        <div class="grid">
            <!-- Session Status -->
            <div class="card">
                <h2>Session Status <button class="btn btn-sm refresh-btn" onclick="checkSession()">Refresh</button></h2>
                <div id="session-status">
                    <div class="loading">Checking session...</div>
                </div>
            </div>

            <!-- Quick Actions -->
            <div class="card">
                <h2>Quick Actions</h2>
                <p style="margin-bottom: 15px; color: var(--text-muted);">Trigger manual job runs</p>
                <div class="job-actions">
                    <button class="btn btn-primary" onclick="runJob('regular')">Run Regular</button>
                    <button class="btn btn-primary" onclick="runJob('super')">Run Super</button>
                    <button class="btn btn-primary" onclick="runJob('regular_gen_super')">Run Gen Super</button>
                </div>
                <div style="margin-top: 15px;">
                    <label>
                        <input type="checkbox" id="clear-progress"> Clear progress before run
                    </label>
                </div>
            </div>
        </div>

        <!-- Running Jobs -->
        <div class="card">
            <h2>Running Jobs <button class="btn btn-sm refresh-btn" onclick="loadJobs()">Refresh</button></h2>
            <div id="jobs-list">
                <div class="loading">Loading jobs...</div>
            </div>
        </div>

        <!-- Update Session -->
        <div class="card">
            <h2>Update Session Cookie</h2>
            <div class="form-group">
                <label>New Cookie Value</label>
                <textarea id="new-cookie" placeholder="Paste your cookie here..."></textarea>
            </div>
            <button class="btn btn-primary" onclick="updateCookie()">Update Cookie</button>
        </div>

        <!-- Filter Settings -->
        <div class="card">
            <h2>Regular Alpha Filters <button class="btn btn-sm refresh-btn" onclick="loadFilters()">Refresh</button></h2>
            <div class="grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
                <div class="form-group">
                    <label>Region</label>
                    <select id="filter-region" style="width: 100%; padding: 10px; border: 1px solid var(--border); border-radius: 6px;">
                        <option value="">Loading...</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Add Custom Region</label>
                    <input type="text" id="filter-custom-region" placeholder="e.g., BRA" maxlength="5" style="text-transform: uppercase;">
                </div>
                <div class="form-group">
                    <label>Date From</label>
                    <input type="datetime-local" id="filter-date-from">
                </div>
                <div class="form-group">
                    <label>Date To</label>
                    <input type="datetime-local" id="filter-date-to">
                </div>
                <div class="form-group">
                    <label>Min Fitness</label>
                    <input type="number" id="filter-min-fitness" step="0.1" min="0" max="10">
                </div>
                <div class="form-group">
                    <label>Limit (max results)</label>
                    <input type="number" id="filter-limit" min="1" max="100">
                </div>
            </div>
            <div style="margin-top: 10px;">
                <label style="display: flex; align-items: center; gap: 8px;">
                    <input type="checkbox" id="filter-favorite">
                    <span>Favorite only</span>
                </label>
            </div>
            <div style="margin-top: 15px;">
                <button class="btn btn-primary" onclick="saveFilters()">Save Filters</button>
                <span id="filter-status" style="margin-left: 15px; color: var(--text-muted);"></span>
            </div>
        </div>

        <!-- Super Alpha Filter Settings -->
        <div class="card">
            <h2>Super Alpha Filters <button class="btn btn-sm refresh-btn" onclick="loadFilters()">Refresh</button></h2>
            <div class="grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
                <div class="form-group">
                    <label>Region</label>
                    <select id="super-filter-region" style="width: 100%; padding: 10px; border: 1px solid var(--border); border-radius: 6px;">
                        <option value="">Loading...</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Date From</label>
                    <input type="datetime-local" id="super-filter-date-from">
                </div>
                <div class="form-group">
                    <label>Date To</label>
                    <input type="datetime-local" id="super-filter-date-to">
                </div>
                <div class="form-group">
                    <label>Limit (max results)</label>
                    <input type="number" id="super-filter-limit" min="1" max="100">
                </div>
            </div>
            <div style="margin-top: 10px;">
                <label style="display: flex; align-items: center; gap: 8px;">
                    <input type="checkbox" id="super-filter-favorite">
                    <span>Favorite only</span>
                </label>
            </div>
            <div style="margin-top: 15px;">
                <button class="btn btn-primary" onclick="saveSuperFilters()">Save Filters</button>
                <span id="super-filter-status" style="margin-left: 15px; color: var(--text-muted);"></span>
            </div>
        </div>

        <!-- Results -->
        <div class="card">
            <h2>Historical Results</h2>
            <div class="tabs">
                <button class="tab active" onclick="showTab('regular')">Regular</button>
                <button class="tab" onclick="showTab('super')">Super</button>
                <button class="tab" onclick="showTab('regular_gen_super')">Gen Super</button>
            </div>
            <div id="results-container">
                <div class="loading">Loading results...</div>
            </div>
        </div>
    </div>

    <script>
        // API calls
        async function api(endpoint, options = {}) {
            try {
                const response = await fetch('/api' + endpoint, {
                    headers: { 'Content-Type': 'application/json' },
                    ...options
                });
                return await response.json();
            } catch (error) {
                console.error('API Error:', error);
                showToast('API Error: ' + error.message, 'error');
                throw error;
            }
        }

        // Session
        async function checkSession() {
            const container = document.getElementById('session-status');
            container.innerHTML = '<div class="loading">Checking...</div>';

            try {
                const data = await api('/session');
                container.innerHTML = `
                    <div class="info-row">
                        <span class="info-label">Status</span>
                        <span class="status-badge status-${data.valid ? 'valid' : 'invalid'}">
                            ${data.valid ? 'Valid' : 'Invalid'}
                        </span>
                    </div>
                    ${data.valid ? `
                        <div class="info-row">
                            <span class="info-label">User</span>
                            <span class="info-value">${data.username}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Email</span>
                            <span class="info-value">${data.email}</span>
                        </div>
                    ` : `
                        <div class="info-row">
                            <span class="info-label">Error</span>
                            <span class="info-value">${data.message}</span>
                        </div>
                    `}
                    <div class="info-row">
                        <span class="info-label">Cookie</span>
                        <span class="info-value" style="font-size: 0.8rem; word-break: break-all;">
                            ${data.cookiePreview}
                        </span>
                    </div>
                `;
            } catch (error) {
                container.innerHTML = '<div class="loading">Error checking session</div>';
            }
        }

        // Update cookie
        async function updateCookie() {
            const cookie = document.getElementById('new-cookie').value.trim();
            if (!cookie) {
                showToast('Please enter a cookie value', 'error');
                return;
            }

            try {
                const data = await api('/session', {
                    method: 'POST',
                    body: JSON.stringify({ cookie })
                });

                if (data.success) {
                    if (data.sessionValid) {
                        showToast('Cookie updated! Session valid for: ' + data.username, 'success');
                    } else {
                        showToast('Cookie updated but session invalid!', 'error');
                    }
                    document.getElementById('new-cookie').value = '';
                    setTimeout(checkSession, 1000);
                } else {
                    showToast('Failed to update cookie', 'error');
                }
            } catch (error) {
                showToast('Error updating cookie', 'error');
            }
        }

        // Jobs
        async function loadJobs() {
            const container = document.getElementById('jobs-list');

            try {
                const data = await api('/jobs');

                if (data.jobs.length === 0) {
                    container.innerHTML = '<p style="color: var(--text-muted);">No jobs running</p>';
                    return;
                }

                container.innerHTML = `
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Started</th>
                                <th>Completed</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.jobs.map(job => `
                                <tr>
                                    <td>${job.id}</td>
                                    <td>${job.type}</td>
                                    <td>
                                        <span class="status-badge status-${job.status.toLowerCase()}">
                                            ${job.status}
                                        </span>
                                    </td>
                                    <td>${formatDate(job.startedAt)}</td>
                                    <td>${job.completedAt ? formatDate(job.completedAt) : '-'}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            } catch (error) {
                container.innerHTML = '<div class="loading">Error loading jobs</div>';
            }
        }

        // Run job
        async function runJob(type) {
            const clearProgress = document.getElementById('clear-progress').checked;

            try {
                const data = await api('/run', {
                    method: 'POST',
                    body: JSON.stringify({ type, clear: clearProgress })
                });

                if (data.success) {
                    showToast(`Job started: ${type} (ID: ${data.jobId})`, 'success');
                    loadJobs();
                } else {
                    showToast('Failed to start job: ' + data.error, 'error');
                }
            } catch (error) {
                showToast('Error starting job', 'error');
            }
        }

        // Results
        let currentTab = 'regular';
        let resultsData = {};

        async function loadResults() {
            try {
                resultsData = await api('/results');
                showTab(currentTab);
            } catch (error) {
                document.getElementById('results-container').innerHTML =
                    '<div class="loading">Error loading results</div>';
            }
        }

        function showTab(tab) {
            currentTab = tab;

            // Update tab buttons
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelector(`.tab:nth-child(${tab === 'regular' ? 1 : tab === 'super' ? 2 : 3})`).classList.add('active');

            // Show content
            const container = document.getElementById('results-container');
            const key = 'progress_' + tab;
            const data = resultsData[key];

            if (!data || typeof data === 'string') {
                container.innerHTML = `<p style="color: var(--text-muted);">No results found for ${tab}</p>`;
                return;
            }

            const alphas = data.alphas || {};
            const alphaList = Object.values(alphas);

            container.innerHTML = `
                <div class="info-row">
                    <span class="info-label">Total Alphas</span>
                    <span class="info-value">${data.totalAlphas || 0}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Processed</span>
                    <span class="info-value">${data.processedCount || 0}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Success</span>
                    <span class="info-value" style="color: var(--success);">${data.successCount || 0}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Failed</span>
                    <span class="info-value" style="color: var(--danger);">${data.failedCount || 0}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Last Updated</span>
                    <span class="info-value">${formatDate(data.lastUpdatedAt)}</span>
                </div>

                <h3 style="margin: 20px 0 10px; font-size: 1rem;">Alpha Details (${alphaList.length})</h3>
                <div class="progress-list">
                    ${alphaList.length === 0 ? '<p style="color: var(--text-muted);">No alphas processed yet</p>' :
                    alphaList
                        .sort((a, b) => (a.correlation || 999) - (b.correlation || 999))
                        .map(alpha => `
                            <div class="alpha-item">
                                <div>
                                    <strong>${alpha.alphaId}</strong>
                                    <span class="status-badge status-${alpha.status?.toLowerCase() || 'pending'}" style="margin-left: 10px;">
                                        ${alpha.status || 'PENDING'}
                                    </span>
                                </div>
                                <div class="correlation ${alpha.correlation < 0.7 ? 'corr-good' : 'corr-bad'}">
                                    ${alpha.correlation !== null && alpha.correlation !== undefined ?
                                        alpha.correlation.toFixed(4) : 'N/A'}
                                </div>
                            </div>
                        `).join('')}
                </div>
            `;
        }

        // Filters
        let filtersData = {};

        async function loadFilters() {
            try {
                filtersData = await api('/filters');

                // Populate region dropdown for Regular Alpha
                const regionSelect = document.getElementById('filter-region');
                regionSelect.innerHTML = '';
                (filtersData.availableRegions || []).forEach(region => {
                    const option = document.createElement('option');
                    option.value = region;
                    option.textContent = region;
                    if (region === filtersData.regular?.region) {
                        option.selected = true;
                    }
                    regionSelect.appendChild(option);
                });

                // Populate other fields for Regular Alpha
                const regular = filtersData.regular || {};

                // Convert ISO date to datetime-local format
                if (regular.dateFrom) {
                    const dateFrom = regular.dateFrom.replace(/-05:00$/, '').replace('T', 'T');
                    document.getElementById('filter-date-from').value = dateFrom.substring(0, 16);
                }
                if (regular.dateTo) {
                    const dateTo = regular.dateTo.replace(/-05:00$/, '').replace('T', 'T');
                    document.getElementById('filter-date-to').value = dateTo.substring(0, 16);
                }

                document.getElementById('filter-min-fitness').value = regular.minFitness || 1.0;
                document.getElementById('filter-limit').value = regular.limit || 5;
                document.getElementById('filter-favorite').checked = regular.favorite || false;

                document.getElementById('filter-status').textContent = '';

                // Populate region dropdown for Super Alpha
                const superRegionSelect = document.getElementById('super-filter-region');
                superRegionSelect.innerHTML = '';
                (filtersData.availableRegions || []).forEach(region => {
                    const option = document.createElement('option');
                    option.value = region;
                    option.textContent = region;
                    if (region === filtersData.super?.region) {
                        option.selected = true;
                    }
                    superRegionSelect.appendChild(option);
                });

                // Populate other fields for Super Alpha
                const superFilters = filtersData.super || {};

                if (superFilters.dateFrom) {
                    const dateFrom = superFilters.dateFrom.replace(/-05:00$/, '').replace('T', 'T');
                    document.getElementById('super-filter-date-from').value = dateFrom.substring(0, 16);
                }
                if (superFilters.dateTo) {
                    const dateTo = superFilters.dateTo.replace(/-05:00$/, '').replace('T', 'T');
                    document.getElementById('super-filter-date-to').value = dateTo.substring(0, 16);
                }

                document.getElementById('super-filter-limit').value = superFilters.limit || 10;
                document.getElementById('super-filter-favorite').checked = superFilters.favorite || false;

                document.getElementById('super-filter-status').textContent = '';

            } catch (error) {
                console.error('Error loading filters:', error);
            }
        }

        async function saveFilters() {
            const statusEl = document.getElementById('filter-status');
            statusEl.textContent = 'Saving...';

            try {
                const dateFrom = document.getElementById('filter-date-from').value;
                const dateTo = document.getElementById('filter-date-to').value;

                const filters = {
                    region: document.getElementById('filter-region').value,
                    customRegion: document.getElementById('filter-custom-region').value,
                    dateFrom: dateFrom ? dateFrom + ':00-05:00' : '',
                    dateTo: dateTo ? dateTo + ':00-05:00' : '',
                    minFitness: document.getElementById('filter-min-fitness').value,
                    limit: document.getElementById('filter-limit').value,
                    favorite: document.getElementById('filter-favorite').checked
                };

                const data = await api('/filters', {
                    method: 'POST',
                    body: JSON.stringify(filters)
                });

                if (data.success) {
                    showToast('Filters saved successfully', 'success');
                    statusEl.textContent = 'Saved!';
                    document.getElementById('filter-custom-region').value = '';
                    // Reload to get updated regions
                    setTimeout(loadFilters, 500);
                } else {
                    showToast('Failed to save filters', 'error');
                    statusEl.textContent = 'Error';
                }
            } catch (error) {
                showToast('Error saving filters', 'error');
                statusEl.textContent = 'Error';
            }
        }

        async function saveSuperFilters() {
            const statusEl = document.getElementById('super-filter-status');
            statusEl.textContent = 'Saving...';

            try {
                const dateFrom = document.getElementById('super-filter-date-from').value;
                const dateTo = document.getElementById('super-filter-date-to').value;

                const filters = {
                    type: 'super',
                    region: document.getElementById('super-filter-region').value,
                    dateFrom: dateFrom ? dateFrom + ':00-05:00' : '',
                    dateTo: dateTo ? dateTo + ':00-05:00' : '',
                    limit: document.getElementById('super-filter-limit').value,
                    favorite: document.getElementById('super-filter-favorite').checked
                };

                const data = await api('/filters', {
                    method: 'POST',
                    body: JSON.stringify(filters)
                });

                if (data.success) {
                    showToast('Super Alpha filters saved successfully', 'success');
                    statusEl.textContent = 'Saved!';
                    setTimeout(loadFilters, 500);
                } else {
                    showToast('Failed to save filters', 'error');
                    statusEl.textContent = 'Error';
                }
            } catch (error) {
                showToast('Error saving filters', 'error');
                statusEl.textContent = 'Error';
            }
        }

        // Utilities
        function formatDate(dateStr) {
            if (!dateStr) return '-';
            const date = new Date(dateStr);
            return date.toLocaleString();
        }

        function showToast(message, type = 'success') {
            const toast = document.createElement('div');
            toast.className = `toast toast-${type}`;
            toast.textContent = message;
            document.body.appendChild(toast);

            setTimeout(() => toast.remove(), 3000);
        }

        // Initialize
        document.addEventListener('DOMContentLoaded', () => {
            checkSession();
            loadJobs();
            loadResults();
            loadFilters();

            // Auto-refresh jobs every 5 seconds
            setInterval(loadJobs, 5000);
        });
    </script>
</body>
</html>
""";
    }
}
