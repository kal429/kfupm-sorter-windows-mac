package io.github.kal429.kfupmsorter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the user chose. Saved as rules.json in the same format as the Windows
 * PowerShell edition, so both editions understand each other's settings.
 */
public final class Settings {

    public static final class Filter {
        public String folder;
        public List<String> keywords = new ArrayList<>();
        public boolean wholeWord = true;

        public Filter(String folder, List<String> keywords, boolean wholeWord) {
            this.folder = folder;
            this.keywords = new ArrayList<>(keywords);
            this.wholeWord = wholeWord;
        }
    }

    /** A folder for one kind of file, such as Documents (.pdf .docx) or Assembly (.asm .s). */
    public static final class TypeDef {
        public String name;
        public List<String> extensions;      // without the dot, lower case: "pdf", "asm"
        public boolean on;

        public TypeDef(String name, List<String> extensions, boolean on) {
            this.name = name;
            this.extensions = new ArrayList<>(extensions);
            this.on = on;
        }

        public TypeDef copy() { return new TypeDef(name, extensions, on); }
    }

    /** The original list (Documents, Images, Videos...). */
    public static List<TypeDef> defaultTypes() {
        List<TypeDef> out = new ArrayList<>();
        for (TypeGroup g : TypeGroup.ALL) out.add(new TypeDef(g.name, g.extensions, g.onByDefault));
        return out;
    }

    /** ".asm, *.S ; inc" becomes [asm, s, inc]. Returns null if one of them is not a valid extension. */
    public static List<String> parseExtensions(String text) {
        List<String> out = new ArrayList<>();
        for (String part : text.split("[,،;\\s]+")) {
            String e = part.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("^\\*?\\.+", "");
            if (e.isEmpty()) continue;
            if (!e.matches("[\\p{L}\\p{N}_+\\-~!]{1,20}")) return null;
            if (!out.contains(e)) out.add(e);
        }
        return out;
    }

    public String watchFolder = AppDirs.defaultWatchFolder().toString();
    public String term = "";
    public boolean termFolder = false;
    public List<String> courses = new ArrayList<>();          // codes such as "COE 301"
    public List<Filter> filters = new ArrayList<>();
    public boolean sortByType = true;
    public List<TypeDef> types = defaultTypes();
    public boolean otherFolder = false;
    /** Files the user moved back into the watched folder are left where they are. */
    public boolean keepUserMoves = true;

    /** Names of the ticked types (what older versions saved as "typeGroups"). */
    public List<String> typeGroups() {
        List<String> out = new ArrayList<>();
        for (TypeDef t : types) if (t.on) out.add(t.name);
        return out;
    }

    // ------------------------------------------------------------ patterns

    /** "COE 301" becomes COE[\s_-]*301(?!\d): matches COE301, coe_301, COE-301, but not COE 3011. */
    public static String coursePattern(String code) {
        Matcher m = Pattern.compile("^([A-Za-z]+)\\s*(\\d+)$").matcher(code.trim());
        if (m.matches()) return m.group(1) + "[\\s_-]*" + m.group(2) + "(?!\\d)";
        return Pattern.quote(code);
    }

    /** A keyword. With wholeWord, "lab" matches "Lab 3 Report.pdf" but not "Syllabus.pdf". */
    public static String keywordPattern(String keyword, boolean wholeWord) {
        String[] words = keyword.trim().split("\\s+");
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < words.length; k++) {
            if (k > 0) b.append("[\\s_-]*");
            b.append(Pattern.quote(words[k]));
        }
        return wholeWord ? "(?<!\\p{L})" + b + "(?!\\p{L})" : b.toString();
    }

    /** Splits "a, b، c" into clean, distinct keywords. */
    public static List<String> splitKeywords(String text) {
        List<String> out = new ArrayList<>();
        for (String part : text.split("[,،;]")) {
            String k = part.trim();
            if (k.isEmpty()) continue;
            boolean dup = false;
            for (String o : out) if (o.equalsIgnoreCase(k)) dup = true;
            if (!dup) out.add(k);
        }
        return out;
    }

    public String courseFolder(String code) {
        String t = term.trim();
        return (termFolder && !t.isEmpty()) ? t + "\\" + code : code;
    }

    // ------------------------------------------------------------ rules.json

    public Map<String, Object> toRules() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("watchFolder", watchFolder);
        r.put("term", term.trim());
        r.put("termFolder", termFolder);
        r.put("minAgeSeconds", 20);
        r.put("sortByTypeIfNoCourseMatch", sortByType);
        r.put("otherFolder", otherFolder ? "Other" : null);
        r.put("skipExtensions", List.of(".crdownload", ".part", ".partial", ".tmp", ".download", ".opdownload", ".!ut", ".lock"));
        r.put("ignoreNames", List.of("desktop.ini", "sorter.log", "lastrun.txt", ".DS_Store"));

        List<Object> custom = new ArrayList<>();
        for (Filter f : filters) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("folder", f.folder);
            m.put("keywords", new ArrayList<Object>(f.keywords));
            m.put("wholeWord", f.wholeWord);
            List<Object> pats = new ArrayList<>();
            for (String k : f.keywords) pats.add(keywordPattern(k, f.wholeWord));
            m.put("patterns", pats);
            custom.add(m);
        }
        r.put("customRules", custom);

        List<Object> courseRules = new ArrayList<>();
        for (String code : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("folder", courseFolder(code));
            m.put("patterns", List.of(coursePattern(code)));
            Catalog.Course c = Catalog.get().find(code);
            m.put("title", c == null ? "" : c.title);
            m.put("credits", c == null ? 0 : c.credits);
            courseRules.add(m);
        }
        r.put("courseRules", courseRules);

        List<Object> typeRules = new ArrayList<>();
        if (sortByType) {
            for (TypeDef t : types) {
                if (!t.on || t.extensions.isEmpty()) continue;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("folder", t.name);
                List<Object> ex = new ArrayList<>();
                for (String e : t.extensions) ex.add("." + e);
                m.put("extensions", ex);
                typeRules.add(m);
            }
        }
        r.put("typeRules", typeRules);
        r.put("keepUserMoves", keepUserMoves);
        // not used by the engine: the full, editable list of types (also the ones that are off)
        List<Object> defs = new ArrayList<>();
        for (TypeDef t : types) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", t.name);
            m.put("extensions", new ArrayList<Object>(t.extensions));
            m.put("on", t.on);
            defs.add(m);
        }
        r.put("typeDefs", defs);
        r.put("typeGroups", new ArrayList<Object>(typeGroups()));
        return r;
    }

    public static Settings fromRules(Map<String, Object> r) {
        Settings s = new Settings();
        s.watchFolder = Json.str(r.get("watchFolder"), s.watchFolder);
        s.term = Json.str(r.get("term"), "");
        s.termFolder = Json.bool(r.get("termFolder"), false);
        s.sortByType = Json.bool(r.get("sortByTypeIfNoCourseMatch"), true);
        s.otherFolder = r.get("otherFolder") != null;

        s.courses.clear();
        for (Object o : Json.arr(r.get("courseRules"))) {
            String folder = Json.str(Json.obj(o).get("folder"), "");
            String code = folder.replaceAll("^.*[\\\\/]", "");
            if (!s.term.isEmpty() && folder.startsWith(s.term + "\\")) s.termFolder = true;
            if (!code.isEmpty() && !s.courses.contains(code)) s.courses.add(code);
        }
        s.filters.clear();
        for (Object o : Json.arr(r.get("customRules"))) {
            Map<String, Object> m = Json.obj(o);
            String folder = Json.str(m.get("folder"), "");
            List<String> kw = new ArrayList<>();
            for (Object k : Json.arr(m.get("keywords"))) kw.add(String.valueOf(k));
            if (!folder.isEmpty() && !kw.isEmpty()) s.filters.add(new Filter(folder, kw, Json.bool(m.get("wholeWord"), true)));
        }
        s.keepUserMoves = Json.bool(r.get("keepUserMoves"), true);
        if (r.containsKey("typeDefs")) {
            s.types = new ArrayList<>();
            for (Object o : Json.arr(r.get("typeDefs"))) {
                Map<String, Object> m = Json.obj(o);
                String name = Json.str(m.get("name"), "");
                List<String> ex = new ArrayList<>();
                for (Object e : Json.arr(m.get("extensions"))) ex.add(String.valueOf(e).replaceAll("^\\.+", "").toLowerCase(java.util.Locale.ROOT));
                if (!name.isEmpty()) s.types.add(new TypeDef(name, ex, Json.bool(m.get("on"), true)));
            }
        } else {
            // settings from version 1.0 (or the PowerShell edition): the original list, ticked as saved
            List<String> groups = new ArrayList<>();
            if (r.containsKey("typeGroups")) {
                for (Object o : Json.arr(r.get("typeGroups"))) groups.add(String.valueOf(o));
            } else {
                for (Object o : Json.arr(r.get("typeRules"))) groups.add(Json.str(Json.obj(o).get("folder"), ""));
            }
            if (!groups.isEmpty() || r.containsKey("typeRules")) {
                for (TypeDef t : s.types) t.on = groups.contains(t.name);
            }
        }
        return s;
    }

    public static Settings load() {
        Path p = AppDirs.rulesFile();
        try {
            if (Files.exists(p)) return fromRules(Json.obj(Json.parse(Files.readString(p, StandardCharsets.UTF_8))));
        } catch (Exception e) {
            SortLog.note("Could not read rules.json: " + e.getMessage());
        }
        return new Settings();
    }

    /** First start on Windows: take over the choices of the PowerShell edition, if there are any. */
    public static Settings importPowershellEdition() {
        Path p = AppDirs.powershellEditionRules();
        try {
            if (p != null && Files.exists(p)) return fromRules(Json.obj(Json.parse(Files.readString(p, StandardCharsets.UTF_8))));
        } catch (Exception ignored) { }
        return null;
    }

    public void save() throws IOException {
        Path p = AppDirs.rulesFile();
        Path tmp = p.resolveSibling("rules.json.tmp");
        Files.writeString(tmp, Json.write(toRules()), StandardCharsets.UTF_8);
        Files.move(tmp, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
