package io.github.kal429.kfupmsorter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Remembers every file the sorter has moved (name, size and modified time).
 * When the user moves such a file back into the watched folder, the sorter
 * recognises it and leaves it where the user put it. A new download of the
 * same file has a new modified time, so it is still sorted as usual.
 * Kept in moved.json in the settings folder, newest 5,000 files.
 */
public final class MovedMemory {
    static final int LIMIT = 5000;

    private final LinkedHashSet<String> keys = new LinkedHashSet<>();
    private boolean changed = false;
    private boolean fresh = true;     // no moved.json yet

    static Path file() { return AppDirs.dataDir().resolve("moved.json"); }

    /** Name (case-insensitive) + size + modified time in milliseconds. */
    public static String key(String name, long size, long modifiedMillis) {
        return name.toLowerCase(Locale.ROOT) + "|" + size + "|" + modifiedMillis;
    }

    public static MovedMemory load() {
        MovedMemory m = new MovedMemory();
        try {
            Path f = file();
            if (Files.exists(f)) {
                m.fresh = false;
                Map<String, Object> root = Json.obj(Json.parse(Files.readString(f, StandardCharsets.UTF_8)));
                for (Object o : Json.arr(root.get("files"))) m.keys.add(String.valueOf(o));
            }
        } catch (Exception e) {
            SortLog.note("Could not read moved.json: " + e.getMessage());
        }
        return m;
    }

    public boolean contains(String key) { return keys.contains(key); }

    /** True the first time (no moved.json yet), so files sorted before this feature can be remembered too. */
    public boolean isFresh() { return fresh; }

    /** Remembers the files already sitting in a sorted folder (used once, on the first run). */
    public void rememberFolder(Path dir) {
        if (!Files.isDirectory(dir)) return;
        try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                java.nio.file.attribute.BasicFileAttributes a = Files.readAttributes(p,
                        java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
                if (!a.isRegularFile()) continue;
                String n = p.getFileName().toString();
                remember(n, n, a.size(), a.lastModifiedTime().toMillis());
            }
        } catch (IOException ignored) { }
        changed = true;
    }

    public int size() { return keys.size(); }

    /** Remembers a move under the original name and under the name it got in the course folder. */
    public void remember(String originalName, String finalName, long size, long modifiedMillis) {
        for (String n : new String[]{originalName, finalName}) {
            String k = key(n, size, modifiedMillis);
            keys.remove(k);   // re-insert at the end: newest last
            keys.add(k);
        }
        while (keys.size() > LIMIT) keys.remove(keys.iterator().next());
        changed = true;
    }

    public void saveIfChanged() {
        if (!changed) return;
        try {
            Path f = file();
            Path tmp = f.resolveSibling("moved.json.tmp");
            Map<String, Object> root = new java.util.LinkedHashMap<>();
            root.put("about", "Files KFUPM Sorter has moved. If you move one back, it stays where you put it.");
            List<Object> list = new ArrayList<>(keys);
            root.put("files", list);
            Files.writeString(tmp, Json.write(root), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
            changed = false;
        } catch (IOException e) {
            SortLog.note("Could not write moved.json: " + e.getMessage());
        }
    }

    /** Forgets everything. Returns how many files were remembered. */
    public static int forget() {
        int n = load().size();
        try { Files.deleteIfExists(file()); } catch (IOException ignored) { }
        return n;
    }
}
