package io.github.kal429.kfupmsorter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Checks GitHub for a newer release. It reads one public page (the latest release of
 * kal429/kfupm-sorter-windows-mac) and sends nothing about the user or their files.
 * Nothing is downloaded or installed by the app itself: the "Update" button opens the
 * installer link in the browser.
 */
public final class Updates {
    private Updates() {}

    public static final String REPO = "kal429/kfupm-sorter-windows-mac";
    static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    public static final String RELEASES_PAGE = "https://github.com/" + REPO + "/releases/latest";

    /** The background sorter asks at most this often. */
    static final long BACKGROUND_EVERY_MS = 24L * 60 * 60 * 1000;
    /** Opening the window asks at most this often; in between it remembers the last answer. */
    static final long WINDOW_EVERY_MS = 3L * 60 * 60 * 1000;

    /** A release newer than this one. */
    public static final class Info {
        public final String version;    // "1.2.0"
        public final String pageUrl;    // the release page (notes, Mac instructions)
        public final String fileUrl;    // the installer for this computer, or the release page
        Info(String version, String pageUrl, String fileUrl) {
            this.version = version;
            this.pageUrl = pageUrl;
            this.fileUrl = fileUrl;
        }
        /** Where the "Update" button goes: the installer on Windows, the release page (with the Terminal step) on a Mac. */
        public String buttonUrl() { return AppDirs.OS_TYPE == AppDirs.OS.MAC ? pageUrl : fileUrl; }
    }

    // ------------------------------------------------------------ asking GitHub

    /**
     * Asks GitHub for the latest release. Returns it when it is newer than this app,
     * null when this app is up to date. Throws when GitHub cannot be reached.
     * The GitHub API allows 60 questions an hour per internet address, which a whole
     * campus network can use up, so a refused answer falls back to the public release page.
     */
    public static Info check() throws Exception {
        Info info;
        try {
            info = fromApi();
        } catch (Exception apiFailed) {
            try {
                info = fromReleasePage();
            } catch (Exception pageFailed) {
                apiFailed.addSuppressed(pageFailed);
                throw apiFailed;
            }
        }
        State s = State.load();
        s.lastCheck = System.currentTimeMillis();
        s.latest = info == null ? "" : info.version;
        s.page = info == null ? "" : info.pageUrl;
        s.file = info == null ? "" : info.fileUrl;
        s.save();
        return info;
    }

    /** The last answer, without asking GitHub again (null when it said this version is current). */
    public static Info remembered() {
        State s = State.load();
        if (s.latest.isEmpty() || compare(s.latest, Main.VERSION) <= 0) return null;
        String page = s.page.startsWith("https://github.com/") ? s.page : RELEASES_PAGE;
        String file = s.file.startsWith("https://github.com/") ? s.file : page;
        return new Info(s.latest, page, file);
    }

    /** True when opening the window should ask GitHub again. */
    public static boolean dueForWindow() {
        long now = System.currentTimeMillis(), last = State.load().lastCheck;
        return now - last >= WINDOW_EVERY_MS || now < last;
    }

    private static HttpClient client(HttpClient.Redirect redirects) {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(redirects).build();
    }

    private static HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("User-Agent", "KFUPM-Sorter/" + Main.VERSION);
    }

    /** The GitHub API: exact, with the installer links. */
    static Info fromApi() throws Exception {
        String url = System.getProperty("kfupmsorter.updateUrl", API);   // tests use a local server
        HttpRequest req = request(url).header("Accept", "application/vnd.github+json").GET().build();
        HttpResponse<String> resp = client(HttpClient.Redirect.NORMAL).send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() == 404) return null;                       // no release published yet
        if (resp.statusCode() != 200) throw new IllegalStateException("GitHub answered " + resp.statusCode());
        return parse(resp.body(), Main.VERSION, assetName());
    }

    /** The public page github.com/.../releases/latest, which redirects to .../releases/tag/v1.2.0. */
    static Info fromReleasePage() throws Exception {
        String url = System.getProperty("kfupmsorter.releasePage", RELEASES_PAGE);
        HttpRequest req = request(url).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> resp = client(HttpClient.Redirect.NEVER).send(req, HttpResponse.BodyHandlers.discarding());
        String where = resp.headers().firstValue("Location").orElse("");
        if (resp.statusCode() / 100 != 3 || where.isEmpty()) throw new IllegalStateException("GitHub release page answered " + resp.statusCode());
        return fromTagUrl(where, Main.VERSION, assetName());
    }

    /** "https://github.com/kal429/kfupm-sorter-windows-mac/releases/tag/v1.2.0" -> that release, if newer. */
    static Info fromTagUrl(String tagUrl, String current, String wantedAsset) {
        String prefix = "https://github.com/" + REPO + "/releases/tag/";
        if (!tagUrl.startsWith(prefix)) return null;
        String tag = tagUrl.substring(prefix.length());
        String version = clean(tag);
        if (version.isEmpty() || compare(version, current) <= 0) return null;
        String file = wantedAsset == null ? tagUrl : "https://github.com/" + REPO + "/releases/download/" + tag + "/" + wantedAsset;
        return new Info(version, tagUrl, file);
    }

    /** Reads a GitHub "release" JSON answer. Returns null for drafts, pre-releases and versions that are not newer. */
    static Info parse(String json, String current, String wantedAsset) {
        Map<String, Object> r = Json.obj(Json.parse(json));
        if (Json.bool(r.get("draft"), false) || Json.bool(r.get("prerelease"), false)) return null;
        String version = clean(Json.str(r.get("tag_name"), ""));
        if (version.isEmpty() || compare(version, current) <= 0) return null;
        String page = Json.str(r.get("html_url"), RELEASES_PAGE);
        if (!page.startsWith("https://github.com/")) page = RELEASES_PAGE;
        String file = page;
        if (wantedAsset != null) {
            for (Object o : Json.arr(r.get("assets"))) {
                Map<String, Object> a = Json.obj(o);
                String link = Json.str(a.get("browser_download_url"), "");
                if (wantedAsset.equals(Json.str(a.get("name"), "")) && link.startsWith("https://github.com/")) {
                    file = link;
                    break;
                }
            }
        }
        return new Info(version, page, file);
    }

    /** "v1.2.0" -> "1.2.0" */
    static String clean(String tag) {
        String t = tag.trim();
        if (t.startsWith("v") || t.startsWith("V")) t = t.substring(1);
        return t.matches("\\d+(\\.\\d+){0,3}") ? t : "";
    }

    /** Compares dotted version numbers: 1.10.0 is newer than 1.9.3. */
    static int compare(String a, String b) {
        String[] x = clean(a).split("\\."), y = clean(b).split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    /** The installer file that fits this computer. */
    static String assetName() { return assetName(AppDirs.OS_TYPE, System.getProperty("os.arch", "")); }

    static String assetName(AppDirs.OS os, String arch) {
        if (os == AppDirs.OS.WINDOWS) return "KFUPM-Sorter-Windows-Setup.exe";
        if (os == AppDirs.OS.MAC) {
            String a = arch.toLowerCase(Locale.ROOT);
            return a.contains("aarch64") || a.contains("arm") ? "KFUPM-Sorter-macOS-AppleSilicon.dmg" : "KFUPM-Sorter-macOS-Intel.dmg";
        }
        return null;
    }

    // ------------------------------------------------------------ what was already asked and shown

    /** update.json in the settings folder. */
    public static final class State {
        public long lastCheck = 0;        // when GitHub was last asked
        public String latest = "";        // newest version seen there ("" = this one is current)
        public String notified = "";      // version the background sorter already announced
        public String page = "";          // its release page
        public String file = "";          // its installer for this computer

        static Path file() { return AppDirs.dataDir().resolve("update.json"); }

        public static State load() {
            State s = new State();
            try {
                Path f = file();
                if (Files.exists(f)) {
                    Map<String, Object> m = Json.obj(Json.parse(Files.readString(f, StandardCharsets.UTF_8)));
                    Object lc = m.get("lastCheck");
                    s.lastCheck = lc instanceof Number ? ((Number) lc).longValue() : 0;
                    s.latest = Json.str(m.get("latest"), "");
                    s.notified = Json.str(m.get("notified"), "");
                    s.page = Json.str(m.get("page"), "");
                    s.file = Json.str(m.get("file"), "");
                }
            } catch (Exception ignored) { }
            return s;
        }

        public void save() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("lastCheck", lastCheck);
            m.put("latest", latest);
            m.put("notified", notified);
            m.put("page", page);
            m.put("file", file);
            try { Files.writeString(file(), Json.write(m), StandardCharsets.UTF_8); } catch (Exception ignored) { }
        }
    }

    /** True when the background sorter should ask GitHub again (at most once a day). */
    static boolean dueInBackground(State s, long now) {
        return now - s.lastCheck >= BACKGROUND_EVERY_MS || now < s.lastCheck;
    }
}
