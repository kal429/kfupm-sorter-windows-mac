package io.github.kal429.kfupmsorter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The KFUPM undergraduate course list: bundled with the app, refreshable from the official bulletin. */
public final class Catalog {

    public static final class Course {
        public final String code, title, subject;
        public final int credits;
        public Course(String code, String title, int credits, String subject) {
            this.code = code; this.title = title; this.credits = credits; this.subject = subject;
        }
        @Override public String toString() { return code + "   -   " + title + "   (" + credits + " cr)"; }
    }

    private static Catalog instance;

    public final List<Course> courses;
    public final String updated;
    public final Map<String, String> subjectNames;
    private final Map<String, Course> byCode = new LinkedHashMap<>();

    private Catalog(List<Course> courses, String updated, Map<String, String> subjectNames) {
        this.courses = courses;
        this.updated = updated;
        this.subjectNames = subjectNames;
        for (Course c : courses) byCode.put(c.code, c);
    }

    public static synchronized Catalog get() {
        if (instance == null) instance = load();
        return instance;
    }

    public static synchronized void reload() { instance = load(); }

    public Course find(String code) { return byCode.get(code); }

    public List<String> subjects() {
        Set<String> s = new TreeSet<>();
        for (Course c : courses) s.add(c.subject);
        return new ArrayList<>(s);
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = Catalog.class.getResourceAsStream("/kfupmsorter/" + name)) {
            if (in == null) throw new IOException("missing resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Catalog load() {
        Map<String, String> names = new LinkedHashMap<>();
        try {
            for (Object o : Json.arr(Json.parse(resource("subjects.json")))) {
                Map<String, Object> m = Json.obj(o);
                names.put(Json.str(m.get("code"), ""), Json.str(m.get("name"), ""));
            }
        } catch (Exception ignored) { }

        String text = null;
        Path updatedFile = AppDirs.catalogFile();
        try { if (Files.exists(updatedFile)) text = Files.readString(updatedFile, StandardCharsets.UTF_8); } catch (IOException ignored) { }
        try { if (text == null) text = resource("courses.json"); } catch (IOException e) { text = "{\"courses\":[]}"; }

        Map<String, Object> root;
        try { root = Json.obj(Json.parse(text)); } catch (Exception e) { root = new LinkedHashMap<>(); }
        List<Course> list = new ArrayList<>();
        for (Object o : Json.arr(root.get("courses"))) {
            Map<String, Object> m = Json.obj(o);
            String code = Json.str(m.get("code"), "");
            if (code.isEmpty()) continue;
            list.add(new Course(code, Json.str(m.get("title"), ""), Json.num(m.get("credits"), 0),
                    Json.str(m.get("subject"), code.split(" ")[0])));
        }
        return new Catalog(list, Json.str(root.get("updated"), ""), names);
    }

    // ------------------------------------------------------------ update from bulletin.kfupm.edu.sa

    // the bulletin prints lines such as:  COE 202 - Digital Logic Design 3-0-3
    private static final Pattern LINE = Pattern.compile(
            "([A-Za-z]{2,5})\\s+(\\d{3})\\s*[-–]\\s*(.{2,95}?)\\s+(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)");

    static List<Course> parse(String html, String subject) {
        String t = html.replaceAll("(?is)<script.*?</script>", " ")
                       .replaceAll("(?is)<style.*?</style>", " ")
                       .replaceAll("(?s)<[^>]+>", " ");
        t = t.replace("&amp;", "&").replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"")
             .replace("&lt;", "<").replace("&gt;", ">").replaceAll("\\s+", " ");
        Map<String, Course> out = new LinkedHashMap<>();
        Matcher m = LINE.matcher(t);
        while (m.find()) {
            String code = m.group(1).toUpperCase();
            if (!code.equals(subject.toUpperCase())) continue;   // stray matches from other text
            String full = code + " " + m.group(2);
            String title = m.group(3).replaceAll("^[\\s\\-–]+|[\\s\\-–]+$", "");
            out.putIfAbsent(full, new Course(full, title, Integer.parseInt(m.group(6)), code));
        }
        return new ArrayList<>(out.values());
    }

    /** Downloads every department's page. progress gets "i/total CODE found". Returns the course count. */
    public static int update(Consumer<String> progress) throws IOException, InterruptedException {
        List<String> codes = new ArrayList<>(new LinkedHashSet<>(get().subjectNames.keySet()));
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        List<Course> all = new ArrayList<>();
        int i = 0;
        for (String code : codes) {
            i++;
            int found = 0;
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(
                        "https://bulletin.kfupm.edu.sa/course-details?subject_code=" + code + "&level=Undergraduate"))
                        .timeout(Duration.ofSeconds(25)).header("User-Agent", "KFUPM-Sorter").GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                List<Course> got = parse(resp.body(), code);
                all.addAll(got);
                found = got.size();
            } catch (IOException e) {
                // one department failing should not stop the rest
            }
            if (progress != null) progress.accept(i + "/" + codes.size() + "  " + code + "  " + found);
        }
        if (all.isEmpty()) throw new IOException("No courses could be read from the bulletin. Check your internet connection.");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("updated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        root.put("source", "bulletin.kfupm.edu.sa");
        root.put("count", all.size());
        List<Object> list = new ArrayList<>();
        for (Course c : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", c.code); m.put("title", c.title); m.put("credits", c.credits); m.put("subject", c.subject);
            list.add(m);
        }
        root.put("courses", list);
        Files.writeString(AppDirs.catalogFile(), Json.write(root), StandardCharsets.UTF_8);
        reload();
        return all.size();
    }
}
