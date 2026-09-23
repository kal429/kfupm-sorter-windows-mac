package io.github.kal429.kfupmsorter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** sorter.log: one line per moved file, same format as the PowerShell edition. */
public final class SortLog {
    private SortLog() {}

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String now() { return LocalDateTime.now().format(STAMP); }

    public static synchronized void note(String text) {
        Path p = AppDirs.logFile();
        try {
            if (Files.exists(p) && Files.size(p) > 1_000_000) {   // keep the log small
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                Files.write(p, lines.subList(lines.size() / 2, lines.size()), StandardCharsets.UTF_8);
            }
            Files.writeString(p, now() + "  " + text + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }

    /** The most recent MOVED / FAILED lines, newest first. */
    public static List<String> recentMoves(int max) {
        Path p = AppDirs.logFile();
        List<String> out = new ArrayList<>();
        try {
            if (!Files.exists(p)) return out;
            for (String l : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                if (l.contains("MOVED") || l.contains("FAILED")) out.add(l.replace("﻿", ""));
            }
        } catch (IOException ignored) { }
        Collections.reverse(out);
        return out.size() > max ? out.subList(0, max) : out;
    }
}
