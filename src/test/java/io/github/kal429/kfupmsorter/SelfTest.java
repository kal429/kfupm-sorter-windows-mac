package io.github.kal429.kfupmsorter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

/**
 * Plain self-test (no libraries): run with
 *   java -Dkfupmsorter.data=<temp dir> -cp <classes> io.github.kal429.kfupmsorter.SelfTest
 * Exits with the number of failures.
 */
public final class SelfTest {
    static int fails = 0, passes = 0;

    static void check(String name, boolean ok) {
        if (ok) { passes++; System.out.println("PASS  " + name); }
        else { fails++; System.out.println("FAIL  " + name); }
    }

    static Path file(Path dir, String name, boolean old) throws Exception {
        Path p = dir.resolve(name);
        Files.writeString(p, "x");
        if (old) {
            FileTime t = FileTime.fromMillis(System.currentTimeMillis() - 120_000);
            Files.setLastModifiedTime(p, t);
            try { Files.setAttribute(p, "basic:creationTime", t); } catch (Exception ignored) { }
        }
        return p;
    }

    public static void main(String[] args) throws Exception {
        Path data = AppDirs.dataDir();
        Path dl = Files.createTempDirectory("kfupm-dl");

        // ---- patterns
        check("course pattern", Settings.coursePattern("COE 301").equals("COE[\\s_-]*301(?!\\d)"));
        check("keyword pattern whole word", Settings.keywordPattern("lab", true).startsWith("(?<!\\p{L})"));
        check("split keywords", Settings.splitKeywords(" arch، cdr ; ARCH, base station ").equals(List.of("arch", "cdr", "base station")));

        // ---- catalog
        check("catalog loaded (2123)", Catalog.get().courses.size() == 2123);
        check("catalog find COE 301", Catalog.get().find("COE 301") != null);
        check("58 departments", Catalog.get().subjects().size() == 58);
        String html = "<div><b>COE 202</b> - Digital Logic Design <span>3-0-3</span></div><p>EE 201 - Other 3-0-3</p>";
        List<Catalog.Course> parsed = Catalog.parse(html, "COE");
        check("bulletin parser", parsed.size() == 1 && parsed.get(0).code.equals("COE 202") && parsed.get(0).credits == 3
                && parsed.get(0).title.equals("Digital Logic Design"));

        // ---- json round trip
        Map<String, Object> m = Json.obj(Json.parse("{\"a\":\"x\\\"y\",\"b\":[1,2.5,true,null],\"c\":{}}"));
        check("json parse", "x\"y".equals(m.get("a")) && Json.arr(m.get("b")).size() == 4);
        check("json write/parse", Json.obj(Json.parse(Json.write(m))).equals(m));
        check("json BOM", Json.obj(Json.parse("﻿{\"k\":1}")).get("k") instanceof Double);

        // ---- settings round trip (same schema as the PowerShell edition)
        Settings s = new Settings();
        s.watchFolder = dl.toString();
        s.term = "261";
        s.termFolder = true;
        s.courses.add("COE 301");
        s.courses.add("EE 236");
        s.filters.add(new Settings.Filter("Internship", List.of("internship", "coop"), true));
        s.filters.add(new Settings.Filter("Club", List.of("ieee"), false));
        s.otherFolder = true;
        s.save();
        Settings back = Settings.load();
        check("rules.json written", Files.exists(data.resolve("rules.json")));
        check("settings round trip", back.courses.equals(s.courses) && back.filters.size() == 2
                && back.termFolder && back.term.equals("261") && back.otherFolder
                && back.filters.get(1).keywords.equals(List.of("ieee")) && !back.filters.get(1).wholeWord);
        Map<String, Object> rules = Json.obj(Json.parse(Files.readString(data.resolve("rules.json"), StandardCharsets.UTF_8)));
        check("term folder prefix", Json.str(Json.obj(Json.arr(rules.get("courseRules")).get(0)).get("folder"), "").equals("261\\COE 301"));

        // PowerShell-edition rules.json (no typeGroups key) is understood
        Settings ps = Settings.fromRules(Json.obj(Json.parse(
                "{\"watchFolder\":\"X\",\"term\":\"261\",\"sortByTypeIfNoCourseMatch\":true,"
              + "\"courseRules\":[{\"folder\":\"261\\\\COE 241\",\"patterns\":[\"x\"]}],"
              + "\"typeRules\":[{\"folder\":\"Videos\"}]}")));
        check("imports PowerShell edition", ps.courses.equals(List.of("COE 241")) && ps.termFolder
                && ps.typeGroups.equals(List.of("Videos")));

        // ---- engine
        file(dl, "Lecture_T261_COE_301_Ch3.pdf", true);
        file(dl, "COE3011 other course.pdf", true);
        file(dl, "Internship_Offer_Letter.pdf", true);
        file(dl, "COOP Report COE301.docx", true);          // custom filter beats course
        file(dl, "research.pdf", true);                      // not "arch"-like; goes by type
        file(dl, "Syllabus internships.txt", true);          // "internship" whole-word: no (internships)
        file(dl, "IEEEmeeting.pptx", true);                  // "ieee" anywhere
        file(dl, "setup.exe", true);
        file(dl, "movie.crdownload", true);                  // unfinished download: skip
        file(dl, "fresh COE301.pdf", false);                 // too new: skip
        file(dl, "~$lock.docx", true);                       // Office lock file: skip
        file(dl, "weird.xyz", true);                         // Other folder
        file(dl, "README", true);                            // no extension: stays
        Files.createDirectories(dl.resolve("Some Folder COE 301"));   // folders never move
        Files.createDirectories(dl.resolve("261").resolve("COE 301"));
        file(dl.resolve("261").resolve("COE 301"), "Lecture_T261_COE_301_Ch3.pdf", true);   // duplicate name

        Engine.Result preview = Engine.runSaved(true);
        check("preview moves nothing", Files.exists(dl.resolve("Lecture_T261_COE_301_Ch3.pdf")) && preview.moves.size() == 9);

        Engine.Result r = Engine.runSaved(false);
        for (Engine.Move mv : r.moves) System.out.println("      moved " + mv.file + " -> " + mv.folder);
        check("course -> term folder, duplicate renamed", Files.exists(dl.resolve("261/COE 301/Lecture_T261_COE_301_Ch3 (1).pdf")));
        check("COE 3011 not COE 301", Files.exists(dl.resolve("Documents/COE3011 other course.pdf")));
        check("custom filter", Files.exists(dl.resolve("Internship/Internship_Offer_Letter.pdf")));
        check("custom beats course", Files.exists(dl.resolve("Internship/COOP Report COE301.docx")));
        check("whole word respected", Files.exists(dl.resolve("Documents/Syllabus internships.txt")));
        check("anywhere keyword", Files.exists(dl.resolve("Club/IEEEmeeting.pptx")));
        check("by type", Files.exists(dl.resolve("Installers/setup.exe")) && Files.exists(dl.resolve("Documents/research.pdf")));
        check("other folder", Files.exists(dl.resolve("Other/weird.xyz")));
        check("crdownload skipped", Files.exists(dl.resolve("movie.crdownload")));
        check("fresh file skipped", Files.exists(dl.resolve("fresh COE301.pdf")));
        check("lock file skipped", Files.exists(dl.resolve("~$lock.docx")));
        check("no extension stays", Files.exists(dl.resolve("README")));
        check("folder not moved", Files.isDirectory(dl.resolve("Some Folder COE 301")));
        check("log written", SortLog.recentMoves(50).size() == 9);
        check("heartbeat", Files.exists(data.resolve("lastrun.txt")));
        check("second run moves nothing", Engine.runSaved(false).moves.isEmpty());

        // ---- autostart file contents (written into a temp home)
        System.out.println();
        System.out.println(passes + " passed, " + fails + " failed");
        System.exit(fails);
    }
}
