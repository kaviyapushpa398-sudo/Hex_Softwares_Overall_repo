import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              URL SHORTENER SERVICE  —  Java (No Deps)           ║
 * ║  Uses only: com.sun.net.httpserver  (built into the JDK)        ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  COMPILE : javac URLShortener.java                              ║
 * ║  RUN     : java URLShortener                                    ║
 * ║  OPEN    : http://localhost:8080                                ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 *  Endpoints
 *  ─────────────────────────────────────────────────────────────────
 *  GET  /               → Web dashboard (HTML UI)
 *  POST /shorten        → body: url=<long>&alias=<optional>
 *  GET  /{code}         → 301 redirect to original URL
 *  GET  /api/list       → JSON list of all short URLs
 *  GET  /api/stats/{c}  → JSON stats for one code
 *  POST /api/delete     → body: code=<code>
 */
public class URLShortener {

    // ── Config ────────────────────────────────────────────────────────────────
    private static final int    PORT        = 8080;
    private static final String BASE_URL    = "http://localhost:" + PORT;
    private static final String DATA_FILE   = "urls.dat";          // persistence
    private static final int    CODE_LENGTH = 6;

    // ── In-memory store ───────────────────────────────────────────────────────
    // code → UrlEntry
    private static final ConcurrentHashMap<String, UrlEntry> store = new ConcurrentHashMap<>();
    private static final AtomicLong totalRedirects = new AtomicLong(0);

    // ── Alphabet for random code generation ──────────────────────────────────
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Date formatter ────────────────────────────────────────────────────────
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    //  DATA MODEL
    // =========================================================================
    static class UrlEntry implements Serializable {
        String  code;
        String  originalUrl;
        String  createdAt;
        long    clicks;
        String  lastAccessed;

        UrlEntry(String code, String originalUrl) {
            this.code        = code;
            this.originalUrl = originalUrl;
            this.createdAt   = LocalDateTime.now().format(FMT);
            this.clicks      = 0;
            this.lastAccessed = "—";
        }

        void recordClick() {
            clicks++;
            lastAccessed = LocalDateTime.now().format(FMT);
            totalRedirects.incrementAndGet();
        }

        String shortUrl() { return BASE_URL + "/" + code; }

        // simple JSON serialisation (no external lib)
        String toJson() {
            return String.format(
                "{\"code\":\"%s\",\"short_url\":\"%s\",\"original_url\":\"%s\"," +
                "\"clicks\":%d,\"created_at\":\"%s\",\"last_accessed\":\"%s\"}",
                code, shortUrl(), escJson(originalUrl),
                clicks, createdAt, lastAccessed);
        }
    }

    // =========================================================================
    //  MAIN
    // =========================================================================
    public static void main(String[] args) throws Exception {
        loadFromDisk();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/",           new RootHandler());
        server.createContext("/shorten",    new ShortenHandler());
        server.createContext("/api/list",   new ListApiHandler());
        server.createContext("/api/stats/", new StatsApiHandler());
        server.createContext("/api/delete", new DeleteApiHandler());

        ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.setExecutor(tpe);
        server.start();

        System.out.println("""

            ╔══════════════════════════════════════════════════╗
            ║        URL SHORTENER SERVICE  STARTED  ✔        ║
            ╠══════════════════════════════════════════════════╣
            ║  Dashboard  →  http://localhost:8080             ║
            ║  Shorten    →  POST /shorten  (url=...)          ║
            ║  Redirect   →  GET  /{code}                      ║
            ║  API List   →  GET  /api/list                    ║
            ║  Press Ctrl+C to stop                            ║
            ╚══════════════════════════════════════════════════╝
            """);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveToDisk();
            server.stop(1);
            tpe.shutdown();
            System.out.println("Server stopped. Data saved.");
        }));
    }

    // =========================================================================
    //  HANDLERS
    // =========================================================================

    /** GET / → HTML dashboard  OR  GET /{code} → redirect */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();

            if (path.equals("/") || path.equals("/index.html")) {
                sendHtml(ex, 200, buildDashboard());
                return;
            }

            // ── Redirect shortcode ───────────────────────────────────────────
            String code = path.substring(1); // strip leading /
            UrlEntry entry = store.get(code);
            if (entry == null) {
                sendHtml(ex, 404, buildNotFound(code));
                return;
            }
            entry.recordClick();
            saveToDisk();

            ex.getResponseHeaders().set("Location", entry.originalUrl);
            ex.sendResponseHeaders(301, -1);
            ex.close();
        }
    }

    /** POST /shorten   body: url=<long_url>&alias=<optional_alias> */
    static class ShortenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> params = parseBody(ex);
            String longUrl  = params.getOrDefault("url", "").trim();
            String alias    = params.getOrDefault("alias", "").trim();

            // ── Validation ───────────────────────────────────────────────────
            if (longUrl.isEmpty()) {
                redirect(ex, "/?error=missing_url");
                return;
            }
            if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
                longUrl = "https://" + longUrl;
            }

            // ── Check for duplicate original URL ─────────────────────────────
            final String finalLong = longUrl;
            Optional<UrlEntry> existing = store.values().stream()
                    .filter(e -> e.originalUrl.equals(finalLong)).findFirst();
            if (existing.isPresent() && alias.isEmpty()) {
                redirect(ex, "/?success=" + existing.get().code + "&dup=1");
                return;
            }

            // ── Resolve code ─────────────────────────────────────────────────
            String code;
            if (!alias.isEmpty()) {
                if (!alias.matches("[a-zA-Z0-9_-]+")) {
                    redirect(ex, "/?error=invalid_alias");
                    return;
                }
                if (store.containsKey(alias)) {
                    redirect(ex, "/?error=alias_taken");
                    return;
                }
                code = alias;
            } else {
                code = generateCode();
            }

            store.put(code, new UrlEntry(code, longUrl));
            saveToDisk();
            redirect(ex, "/?success=" + code);
        }
    }

    /** GET /api/list → JSON array */
    static class ListApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String json = store.values().stream()
                    .sorted(Comparator.comparingLong((UrlEntry e) -> e.clicks).reversed())
                    .map(UrlEntry::toJson)
                    .collect(Collectors.joining(",", "[", "]"));
            sendJson(ex, 200, json);
        }
    }

    /** GET /api/stats/{code} → JSON */
    static class StatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String code = ex.getRequestURI().getPath().replaceFirst("/api/stats/", "");
            UrlEntry entry = store.get(code);
            if (entry == null) { sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            sendJson(ex, 200, entry.toJson());
        }
    }

    /** POST /api/delete   body: code=<code> */
    static class DeleteApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                redirect(ex, "/"); return;
            }
            Map<String, String> params = parseBody(ex);
            String code = params.getOrDefault("code", "").trim();
            store.remove(code);
            saveToDisk();
            redirect(ex, "/?deleted=" + code);
        }
    }

    // =========================================================================
    //  HTML BUILDER
    // =========================================================================
    private static String buildDashboard() {
        // ── Stats bar ────────────────────────────────────────────────────────
        long totalUrls = store.size();
        long totalClicks = store.values().stream().mapToLong(e -> e.clicks).sum();

        // ── URL table rows ───────────────────────────────────────────────────
        StringBuilder rows = new StringBuilder();
        List<UrlEntry> sorted = store.values().stream()
                .sorted(Comparator.comparing((UrlEntry e) -> e.createdAt).reversed())
                .toList();

        for (UrlEntry e : sorted) {
            String display = e.originalUrl.length() > 50
                    ? e.originalUrl.substring(0, 47) + "…"
                    : e.originalUrl;
            rows.append(String.format("""
                <tr>
                  <td><a class="short-link" href="/%s" target="_blank">%s/%s</a>
                      <button class="copy-btn" onclick="copyText('%s/%s')" title="Copy">⧉</button></td>
                  <td><a class="orig-link" href="%s" target="_blank" title="%s">%s</a></td>
                  <td class="badge">%d</td>
                  <td class="muted">%s</td>
                  <td class="muted">%s</td>
                  <td>
                    <form method="POST" action="/api/delete" onsubmit="return confirm('Delete this link?')">
                      <input type="hidden" name="code" value="%s">
                      <button class="del-btn" type="submit">✕</button>
                    </form>
                  </td>
                </tr>
                """,
                e.code, BASE_URL, e.code, BASE_URL, e.code,
                escHtml(e.originalUrl), escHtml(e.originalUrl), escHtml(display),
                e.clicks,
                e.createdAt, e.lastAccessed, e.code));
        }

        return HTML_TEMPLATE
                .replace("{{BASE_URL}}", BASE_URL)
                .replace("{{TOTAL_URLS}}", String.valueOf(totalUrls))
                .replace("{{TOTAL_CLICKS}}", String.valueOf(totalClicks))
                .replace("{{TOTAL_REDIRECTS}}", String.valueOf(totalRedirects.get()))
                .replace("{{ROWS}}", rows.toString());
    }

    private static String buildNotFound(String code) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <title>404 – Not Found</title>
            <style>
              body{font-family:system-ui;display:flex;flex-direction:column;
                   align-items:center;justify-content:center;height:100vh;
                   background:#0f172a;color:#e2e8f0;margin:0}
              h1{font-size:5rem;margin:0;color:#f87171}
              p{color:#94a3b8;font-size:1.1rem}
              a{color:#38bdf8;text-decoration:none;font-weight:600}
              a:hover{text-decoration:underline}
            </style></head><body>
            <h1>404</h1>
            <p>No short URL found for code <code style="color:#fbbf24">""" + escHtml(code) + """
            </code></p>
            <p><a href="/">← Go back to dashboard</a></p>
            </body></html>""";
    }

    // ── The full HTML/CSS/JS dashboard template ───────────────────────────────
    private static final String HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>⚡ URL Shortener</title>
          <style>
            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

            body {
              font-family: 'Segoe UI', system-ui, sans-serif;
              background: #0f172a;
              color: #e2e8f0;
              min-height: 100vh;
            }

            /* ── TOP NAV ─────────────────────────────────── */
            nav {
              background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
              border-bottom: 1px solid #334155;
              padding: 1rem 2rem;
              display: flex;
              align-items: center;
              gap: .75rem;
            }
            nav .logo { font-size: 1.6rem; font-weight: 800;
              background: linear-gradient(90deg,#38bdf8,#818cf8);
              -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
            nav .sub  { color: #64748b; font-size: .85rem; }

            /* ── MAIN LAYOUT ─────────────────────────────── */
            main { max-width: 1100px; margin: 2rem auto; padding: 0 1.5rem; }

            /* ── SHORTEN CARD ────────────────────────────── */
            .shorten-card {
              background: #1e293b;
              border: 1px solid #334155;
              border-radius: 1rem;
              padding: 2rem;
              margin-bottom: 2rem;
              box-shadow: 0 4px 32px rgba(0,0,0,.4);
            }
            .shorten-card h2 { font-size: 1.2rem; color: #94a3b8; margin-bottom: 1.25rem; }

            .form-row {
              display: flex;
              gap: .75rem;
              flex-wrap: wrap;
            }
            .form-row input {
              background: #0f172a;
              border: 1px solid #475569;
              border-radius: .5rem;
              color: #e2e8f0;
              padding: .75rem 1rem;
              font-size: 1rem;
              outline: none;
              transition: border .2s;
            }
            .form-row input:focus { border-color: #38bdf8; }
            .form-row input.url-input { flex: 1 1 300px; }
            .form-row input.alias-input { flex: 0 1 160px; }

            .btn-primary {
              background: linear-gradient(135deg, #0ea5e9, #6366f1);
              color: #fff;
              border: none;
              border-radius: .5rem;
              padding: .75rem 1.75rem;
              font-size: 1rem;
              font-weight: 600;
              cursor: pointer;
              transition: opacity .2s, transform .15s;
            }
            .btn-primary:hover { opacity: .88; transform: translateY(-1px); }

            /* ── TOAST MESSAGES ──────────────────────────── */
            .toast {
              display: flex;
              align-items: center;
              gap: .6rem;
              padding: .85rem 1.2rem;
              border-radius: .6rem;
              font-size: .95rem;
              font-weight: 500;
              margin-bottom: 1.25rem;
              animation: fadeIn .3s;
            }
            .toast.success { background:#052e16; border:1px solid #16a34a; color:#4ade80; }
            .toast.error   { background:#2d0d0d; border:1px solid #dc2626; color:#f87171; }
            .toast.info    { background:#0c1a2e; border:1px solid #0284c7; color:#38bdf8; }
            @keyframes fadeIn { from{opacity:0;transform:translateY(-6px)} to{opacity:1;transform:translateY(0)} }

            /* ── STATS STRIP ─────────────────────────────── */
            .stats-strip {
              display: flex;
              gap: 1rem;
              margin-bottom: 2rem;
              flex-wrap: wrap;
            }
            .stat-box {
              flex: 1 1 160px;
              background: #1e293b;
              border: 1px solid #334155;
              border-radius: .75rem;
              padding: 1.2rem 1.5rem;
              text-align: center;
            }
            .stat-box .num { font-size: 2.2rem; font-weight: 800; color: #38bdf8; }
            .stat-box .lbl { color: #64748b; font-size: .8rem; text-transform: uppercase;
                             letter-spacing: .08em; margin-top: .2rem; }

            /* ── TABLE ───────────────────────────────────── */
            .table-card {
              background: #1e293b;
              border: 1px solid #334155;
              border-radius: 1rem;
              overflow: hidden;
              box-shadow: 0 4px 32px rgba(0,0,0,.3);
            }
            .table-card h2 {
              padding: 1.25rem 1.5rem;
              color: #94a3b8;
              font-size: 1rem;
              border-bottom: 1px solid #334155;
            }

            table { width: 100%; border-collapse: collapse; }
            thead th {
              background: #0f172a;
              color: #64748b;
              font-size: .75rem;
              text-transform: uppercase;
              letter-spacing: .08em;
              padding: .75rem 1rem;
              text-align: left;
            }
            tbody tr { border-bottom: 1px solid #1e293b; transition: background .15s; }
            tbody tr:hover { background: #162032; }
            tbody td { padding: .85rem 1rem; font-size: .92rem; vertical-align: middle; }

            .short-link {
              color: #38bdf8;
              font-weight: 600;
              text-decoration: none;
              font-size: .9rem;
            }
            .short-link:hover { text-decoration: underline; }

            .orig-link {
              color: #94a3b8;
              text-decoration: none;
              font-size: .85rem;
            }
            .orig-link:hover { color: #e2e8f0; }

            .badge {
              background: #0ea5e920;
              color: #38bdf8;
              border-radius: .4rem;
              padding: .2rem .6rem;
              font-weight: 700;
              font-size: .85rem;
              width: 60px;
              text-align: center;
            }
            .muted { color: #475569; font-size: .78rem; }

            .copy-btn {
              background: none;
              border: none;
              color: #475569;
              cursor: pointer;
              font-size: .9rem;
              padding: .1rem .3rem;
              border-radius: .3rem;
              transition: color .15s, background .15s;
              margin-left: .25rem;
            }
            .copy-btn:hover { color: #38bdf8; background: #0ea5e910; }

            .del-btn {
              background: none;
              border: 1px solid #475569;
              color: #94a3b8;
              border-radius: .35rem;
              cursor: pointer;
              padding: .2rem .55rem;
              font-size: .8rem;
              transition: all .15s;
            }
            .del-btn:hover { background: #f8717130; border-color: #f87171; color: #f87171; }

            .empty-state {
              text-align: center;
              padding: 3rem;
              color: #475569;
              font-size: 1rem;
            }
            .empty-state .icon { font-size: 2.5rem; display: block; margin-bottom: .5rem; }

            /* ── API HINT ─────────────────────────────────── */
            .api-hint {
              margin-top: 1.5rem;
              background: #1e293b;
              border: 1px solid #334155;
              border-radius: .75rem;
              padding: 1.25rem 1.5rem;
              font-size: .85rem;
              color: #64748b;
            }
            .api-hint code {
              background: #0f172a;
              border: 1px solid #334155;
              border-radius: .3rem;
              padding: .1rem .4rem;
              color: #a5f3fc;
              font-family: monospace;
              font-size: .82rem;
            }
            .api-hint h3 { color: #94a3b8; margin-bottom: .6rem; font-size: .9rem; }
            .api-hint ul { padding-left: 1.1rem; }
            .api-hint li { margin-bottom: .35rem; line-height: 1.7; }

            footer {
              text-align: center;
              color: #334155;
              padding: 2rem;
              font-size: .8rem;
            }
          </style>
        </head>
        <body>

        <nav>
          <span class="logo">⚡ ShortURL</span>
          <span class="sub">— Java URL Shortening Service</span>
        </nav>

        <main>

          <!-- ── TOAST ─────────────────────────────────────────────────── -->
          <div id="toast-area"></div>

          <!-- ── SHORTEN FORM ──────────────────────────────────────────── -->
          <div class="shorten-card">
            <h2>✂️  Shorten a URL</h2>
            <form class="form-row" method="POST" action="/shorten">
              <input class="url-input"   type="url"  name="url"
                     placeholder="https://example.com/very/long/url" required>
              <input class="alias-input" type="text" name="alias"
                     placeholder="custom-alias (opt.)"
                     pattern="[a-zA-Z0-9_-]*" maxlength="20"
                     title="Letters, digits, hyphens, underscores only">
              <button class="btn-primary" type="submit">Shorten →</button>
            </form>
          </div>

          <!-- ── STATS STRIP ────────────────────────────────────────────── -->
          <div class="stats-strip">
            <div class="stat-box">
              <div class="num">{{TOTAL_URLS}}</div>
              <div class="lbl">Short URLs</div>
            </div>
            <div class="stat-box">
              <div class="num">{{TOTAL_CLICKS}}</div>
              <div class="lbl">Total Clicks</div>
            </div>
            <div class="stat-box">
              <div class="num">{{TOTAL_REDIRECTS}}</div>
              <div class="lbl">Redirects (session)</div>
            </div>
          </div>

          <!-- ── URL TABLE ─────────────────────────────────────────────── -->
          <div class="table-card">
            <h2>📋  All Short Links</h2>
            <table>
              <thead>
                <tr>
                  <th>Short URL</th>
                  <th>Original URL</th>
                  <th>Clicks</th>
                  <th>Created</th>
                  <th>Last Accessed</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {{ROWS}}
              </tbody>
            </table>
            {{EMPTY_STATE}}
          </div>

          <!-- ── API REFERENCE ─────────────────────────────────────────── -->
          <div class="api-hint">
            <h3>🔌 REST API</h3>
            <ul>
              <li><code>POST /shorten</code> body: <code>url=&lt;long&gt;&amp;alias=&lt;opt&gt;</code> — create short link</li>
              <li><code>GET  /{code}</code> — redirect to original URL (301)</li>
              <li><code>GET  /api/list</code> — JSON array of all links</li>
              <li><code>GET  /api/stats/{code}</code> — JSON stats for one link</li>
              <li><code>POST /api/delete</code> body: <code>code=&lt;code&gt;</code> — delete a link</li>
            </ul>
          </div>

        </main>

        <footer>⚡ ShortURL — Built with Java HttpServer · No frameworks · {{BASE_URL}}</footer>

        <script>
          // ── Parse query params for toast messages ──────────────────────────
          const params = new URLSearchParams(location.search);

          function showToast(msg, type) {
            const area = document.getElementById('toast-area');
            const div  = document.createElement('div');
            div.className = 'toast ' + type;
            div.textContent = msg;
            area.prepend(div);
            setTimeout(() => div.remove(), 5000);
          }

          if (params.get('success')) {
            const code = params.get('success');
            const dup  = params.get('dup') === '1';
            showToast(
              (dup ? '⚠ URL already exists → ' : '✔ Shortened! → ') +
              '{{BASE_URL}}/' + code,
              'success'
            );
          }
          if (params.get('deleted')) {
            showToast('🗑 Deleted short code: ' + params.get('deleted'), 'info');
          }
          const errMap = {
            missing_url   : '⚠ Please enter a URL.',
            invalid_alias : '⚠ Alias can only contain letters, digits, hyphens and underscores.',
            alias_taken   : '⚠ That alias is already in use. Try a different one.',
          };
          if (params.get('error') && errMap[params.get('error')]) {
            showToast(errMap[params.get('error')], 'error');
          }

          // Remove query string from address bar without reload
          if (location.search) history.replaceState({}, '', location.pathname);

          // ── Copy to clipboard ──────────────────────────────────────────────
          function copyText(text) {
            navigator.clipboard.writeText(text).then(() => {
              showToast('📋 Copied: ' + text, 'info');
            });
          }
        </script>
        </body>
        </html>
        """;

    // =========================================================================
    //  PERSISTENCE  (simple CSV-style flat file)
    // =========================================================================
    private static void saveToDisk() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE))) {
            pw.println("# URLShortener data — do not edit manually");
            pw.println("# code|originalUrl|createdAt|clicks|lastAccessed");
            for (UrlEntry e : store.values()) {
                pw.printf("%s|%s|%s|%d|%s%n",
                        e.code, e.originalUrl, e.createdAt, e.clicks, e.lastAccessed);
            }
        } catch (IOException ex) {
            System.err.println("[WARN] Could not save data: " + ex.getMessage());
        }
    }

    private static void loadFromDisk() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int loaded = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) continue;
                String[] parts = line.split("\\|", 5);
                if (parts.length < 5) continue;
                UrlEntry e = new UrlEntry(parts[0], parts[1]);
                e.createdAt    = parts[2];
                e.clicks       = Long.parseLong(parts[3]);
                e.lastAccessed = parts[4];
                store.put(e.code, e);
                loaded++;
            }
            System.out.printf("[INFO] Loaded %d URL(s) from %s%n", loaded, DATA_FILE);
        } catch (Exception ex) {
            System.err.println("[WARN] Could not load data: " + ex.getMessage());
        }
    }

    // =========================================================================
    //  UTILITIES
    // =========================================================================
    private static String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++)
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            code = sb.toString();
        } while (store.containsKey(code));
        return code;
    }

    /** Parse application/x-www-form-urlencoded body */
    private static Map<String, String> parseBody(HttpExchange ex) throws IOException {
        byte[] raw = ex.getRequestBody().readAllBytes();
        String body = new String(raw, StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    /** Escape HTML special chars */
    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;");
    }

    /** Escape JSON string content */
    private static String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r");
    }
}