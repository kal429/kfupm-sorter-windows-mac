package io.github.kal429.kfupmsorter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The sorter. Same rules as the Windows PowerShell edition:
 * custom filters first, then courses, then file types, then the optional "Other" folder.
 * It never deletes and never overwrites: a duplicate name becomes "name (1).ext".
 */
public final class Engine {

    public static final class Move {
        public final String file;
        public final String folder;
        Move(String file, String folder) { this.file = file; this.folder = folder; }
    }

    public static final class Result {
        public final List<Move> moves = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public boolean skippedBusy = false;   // another sort was already running
    }

    private static final class Rule {
        final String folder;
        final Pattern pattern;
        Rule(String folder, Pattern pattern) { this.folder = folder; this.pattern = pattern; }
    }

    private final Path root;
    private final List<Rule> nameRules = new ArrayList<>();
    private final List<String[]> typeRules = new ArrayList<>();    // [folder, ".ext", ".ext"...]
    private final boolean sortByType;
    private final String otherFolder;
    private final Set<String> skipExt = new HashSet<>();
    private final Set<String> ignoreNames = new HashSet<>();
    private final long minAgeMillis;

    public Engine(Map<String, Object> rules) {
        root = Paths.get(Json.str(rules.get("watchFolder"), AppDirs.defaultWatchFolder().toString()));
        addNameRules(rules.get("customRules"));     // custom filters win over courses
        addNameRules(rules.get("courseRules"));
        sortByType = Json.bool(rules.get("sortByTypeIfNoCourseMatch"), true);
        otherFolder = rules.get("otherFolder") == null ? null : String.valueOf(rules.get("otherFolder"));
        for (Object o : Json.arr(rules.get("typeRules"))) {
            Map<String, Object> m = Json.obj(o);
            List<Object> ex = Json.arr(m.get("extensions"));
            String[] row = new String[ex.size() + 1];
            row[0] = Json.str(m.get("folder"), "Other");
            for (int k = 0; k < ex.size(); k++) row[k + 1] = String.valueOf(ex.get(k)).toLowerCase(Locale.ROOT);
            typeRules.add(row);
        }
        for (Object o : Json.arr(rules.get("skipExtensions"))) skipExt.add(String.valueOf(o).toLowerCase(Locale.ROOT));
        for (Object o : Json.arr(rules.get("ignoreNames"))) ignoreNames.add(String.valueOf(o).toLowerCase(Locale.ROOT));
        minAgeMillis = 1000L * Json.num(rules.get("minAgeSeconds"), 20);
    }

    private void addNameRules(Object list) {
        for (Object o : Json.arr(list)) {
            Map<String, Object> m = Json.obj(o);
            String folder = Json.str(m.get("folder"), "");
            if (folder.isEmpty()) continue;
            for (Object p : Json.arr(m.get("patterns"))) {
                try {
                    nameRules.add(new Rule(folder, Pattern.compile(String.valueOf(p), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
                } catch (Exception e) {
                    SortLog.note("Skipped a bad pattern for " + folder + ": " + p);
                }
            }
        }
    }

    public Path root() { return root; }

    static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    /** Folder (relative to the watched folder) a file belongs in, or null to leave it alone. */
    public String destination(String fileName) {
        for (Rule r : nameRules) {
            if (r.pattern.matcher(fileName).find()) return r.folder;
        }
        if (!sortByType) return null;
        String ext = extension(fileName);
        if (ext.isEmpty()) return null;
        for (String[] row : typeRules) {
            for (int k = 1; k < row.length; k++) if (row[k].equals(ext)) return row[0];
        }
        return otherFolder;
    }

    static Path uniqueTarget(Path dir, String name) {
        Path candidate = dir.resolve(name);
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) return candidate;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; ; i++) {
            candidate = dir.resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) return candidate;
        }
    }

    /** One pass over the files directly inside the watched folder (never its subfolders). */
    public Result run(boolean dryRun) {
        Result res = new Result();
        if (!Files.isDirectory(root)) {
            res.errors.add("Folder not found: " + root);
            return res;
        }
        // only one sort at a time (the window and the background service may both try)
        try (RandomAccessFile raf = new RandomAccessFile(AppDirs.dataDir().resolve("sort.lock").toFile(), "rw");
             FileChannel ch = raf.getChannel();
             FileLock lock = ch.tryLock()) {
            if (lock == null) { res.skippedBusy = true; return res; }
            sortFiles(res, dryRun);
        } catch (IOException | java.nio.channels.OverlappingFileLockException e) {
            res.skippedBusy = true;
            return res;
        }
        if (!dryRun) writeHeartbeat(res.moves.size());
        return res;
    }

    private void sortFiles(Result res, boolean dryRun) {
        long now = System.currentTimeMillis();
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path p : ds) files.add(p);
        } catch (IOException e) {
            res.errors.add("Could not read " + root + ": " + e.getMessage());
            return;
        }
        files.sort(null);
        for (Path p : files) {
            String name = p.getFileName().toString();
            try {
                BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!a.isRegularFile()) continue;                          // folders, links, app bundles
                if (name.startsWith(".") || name.startsWith("~$")) continue;  // hidden and Office lock files
                if (ignoreNames.contains(name.toLowerCase(Locale.ROOT))) continue;
                if (skipExt.contains(extension(name))) continue;          // unfinished downloads
                if (now - a.lastModifiedTime().toMillis() < minAgeMillis) continue;   // still arriving
            } catch (IOException e) {
                continue;
            }
            String rel = destination(name);
            if (rel == null) continue;
            if (dryRun) { res.moves.add(new Move(name, rel)); continue; }
            Path dir = root;
            for (String part : rel.split("[\\\\/]")) if (!part.isEmpty()) dir = dir.resolve(part);
            try {
                Files.createDirectories(dir);
                Path target = uniqueTarget(dir, name);
                Files.move(p, target);                                    // no REPLACE_EXISTING: never overwrite
                res.moves.add(new Move(name, rel));
                SortLog.note("MOVED   " + name + "  ->  " + rel);
            } catch (NoSuchFileException e) {
                // the file went away on its own
            } catch (FileSystemException e) {
                // usually "being used by another process": try again next time, quietly
            } catch (IOException e) {
                res.errors.add(name + ": " + e.getMessage());
                SortLog.note("FAILED  " + name + "  ->  " + rel + "  (" + e.getMessage() + ")");
            }
        }
    }

    private static void writeHeartbeat(int moved) {
        String text = "last run : " + SortLog.now() + System.lineSeparator() + "moved    : " + moved + System.lineSeparator();
        try { Files.writeString(AppDirs.lastRunFile(), text, StandardCharsets.UTF_8); } catch (IOException ignored) { }
    }

    /** Sort with the saved settings. */
    public static Result runSaved(boolean dryRun) {
        return new Engine(Settings.load().toRules()).run(dryRun);
    }
}
