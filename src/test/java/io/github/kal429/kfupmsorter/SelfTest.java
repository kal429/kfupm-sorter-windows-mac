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
                && ps.typeGroups().equals(List.of("Videos")));

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

        // ---- 1.1: files the user moved back stay where they are
        Path sorted = dl.resolve("Internship/Internship_Offer_Letter.pdf");
        Files.move(sorted, dl.resolve("Internship_Offer_Letter.pdf"));        // the user takes it back out
        Path dup = dl.resolve("261/COE 301/Lecture_T261_COE_301_Ch3 (1).pdf");
        Files.move(dup, dl.resolve("Lecture_T261_COE_301_Ch3 (1).pdf"));       // also under its "(1)" name
        Engine.Result back1 = Engine.runSaved(false);
        check("moved-back files are left alone", back1.moves.isEmpty()
                && Files.exists(dl.resolve("Internship_Offer_Letter.pdf"))
                && Files.exists(dl.resolve("Lecture_T261_COE_301_Ch3 (1).pdf")));
        check("moved.json written", Files.exists(data.resolve("moved.json")));
        // a new download with the same name (new time) is still sorted
        Files.move(dl.resolve("Internship_Offer_Letter.pdf"), dl.resolve("Internship/Internship_Offer_Letter.pdf"));
        Path fresh2 = file(dl, "Internship_Offer_Letter.pdf", false);
        Files.setLastModifiedTime(fresh2, FileTime.fromMillis(System.currentTimeMillis() - 60_000));
        Engine.runSaved(false);
        check("new download with the same name is sorted", Files.exists(dl.resolve("Internship/Internship_Offer_Letter (1).pdf")));
        // option off: sorted again
        s.keepUserMoves = false;
        s.save();
        Engine.runSaved(false);
        check("option off sorts it again", Files.exists(dl.resolve("261/COE 301/Lecture_T261_COE_301_Ch3 (1).pdf"))
                && !Files.exists(dl.resolve("Lecture_T261_COE_301_Ch3 (1).pdf")));
        s.keepUserMoves = true;
        s.save();
        check("forget clears the memory", MovedMemory.forget() > 0 && !Files.exists(data.resolve("moved.json")));
        // first run of 1.1 learns what is already sorted (files sorted by 1.0)
        Engine.runSaved(false);                                                 // learns
        Files.move(dl.resolve("Club/IEEEmeeting.pptx"), dl.resolve("IEEEmeeting.pptx"));
        check("files sorted before the update count too", Engine.runSaved(false).moves.isEmpty()
                && Files.exists(dl.resolve("IEEEmeeting.pptx")));

        // ---- 1.1: editable file types
        check("parse extensions", Settings.parseExtensions(".asm, *.S ; inc  .ASM").equals(List.of("asm", "s", "inc"))
                && Settings.parseExtensions("a/b") == null && Settings.parseExtensions(" ").isEmpty());
        Settings t = Settings.load();
        for (Settings.TypeDef td : t.types) td.extensions.remove("asm");
        t.types.add(new Settings.TypeDef("Assembly", List.of("asm", "s"), true));
        t.save();
        Settings t2 = Settings.load();
        Settings.TypeDef asm = t2.types.get(t2.types.size() - 1);
        check("custom type saved", asm.name.equals("Assembly") && asm.extensions.equals(List.of("asm", "s")) && asm.on
                && t2.types.size() == TypeGroup.ALL.size() + 1);
        file(dl, "lab1.asm", true);
        file(dl, "boot.S", true);
        Engine.runSaved(false);
        check("assembly files go to their own folder", Files.exists(dl.resolve("Assembly/lab1.asm")) && Files.exists(dl.resolve("Assembly/boot.S")));
        Settings old = Settings.fromRules(Json.obj(Json.parse("{\"typeGroups\":[\"Documents\",\"Code\"],\"typeRules\":[]}")));
        check("1.0 settings still load", old.typeGroups().equals(List.of("Documents", "Code")) && old.types.size() == TypeGroup.ALL.size());
        t2.types.get(0).on = false;     // Documents off
        t2.save();
        file(dl, "notes.pdf", true);
        Engine.runSaved(false);
        check("unticked type is not sorted", !Files.exists(dl.resolve("Documents/notes.pdf")) && Files.exists(dl.resolve("Other/notes.pdf")));

        // ---- 1.1: a filter added later can pick up files already sorted by type
        Files.createDirectories(dl.resolve("Documents"));
        file(dl.resolve("Documents"), "ARCh2027_Silicon_Battery_Proposal.pdf", true);
        file(dl.resolve("Documents"), "research_paper.pdf", true);
        Settings f2 = Settings.load();
        f2.filters.add(new Settings.Filter("ARCH", List.of("ARCh2027", "ARCH"), true));
        f2.save();
        Engine eng = new Engine(f2.toRules());
        List<Engine.Resort> rs = eng.findResorts(null);
        check("finds the old file for the new filter (" + rs.size() + ")", rs.size() == 1
                && rs.get(0).file.equals("ARCh2027_Silicon_Battery_Proposal.pdf") && rs.get(0).from.equals("Documents") && rs.get(0).to.equals("ARCH"));
        check("only the asked folders", eng.findResorts(java.util.Set.of("Internship")).isEmpty());
        check("moves it", eng.applyResorts(rs) == 1 && Files.exists(dl.resolve("ARCH/ARCh2027_Silicon_Battery_Proposal.pdf"))
                && Files.exists(dl.resolve("Documents/research_paper.pdf")));

        // ---- 1.2: new-version check
        check("version compare", Updates.compare("v1.10.0", "1.9.3") > 0 && Updates.compare("1.1.1", "v1.1.1") == 0
                && Updates.compare("1.1", "1.1.1") < 0 && Updates.compare("2.0.0", "1.99.99") > 0);
        check("tag clean-up", Updates.clean("v1.2.0").equals("1.2.0") && Updates.clean("nightly").isEmpty());
        check("installer per computer", "KFUPM-Sorter-Windows-Setup.exe".equals(Updates.assetName(AppDirs.OS.WINDOWS, "amd64"))
                && "KFUPM-Sorter-macOS-AppleSilicon.dmg".equals(Updates.assetName(AppDirs.OS.MAC, "aarch64"))
                && "KFUPM-Sorter-macOS-Intel.dmg".equals(Updates.assetName(AppDirs.OS.MAC, "x86_64"))
                && Updates.assetName(AppDirs.OS.OTHER, "amd64") == null);
        String rel = "{\"tag_name\":\"v9.0.0\",\"draft\":false,\"prerelease\":false,"
                + "\"html_url\":\"https://github.com/kal429/kfupm-sorter-windows-mac/releases/tag/v9.0.0\","
                + "\"assets\":[{\"name\":\"KFUPM-Sorter-macOS-Intel.dmg\",\"browser_download_url\":\"https://github.com/x/intel.dmg\"},"
                + "{\"name\":\"KFUPM-Sorter-Windows-Setup.exe\",\"browser_download_url\":\"https://github.com/x/setup.exe\"}]}";
        Updates.Info newer = Updates.parse(rel, "1.1.1", "KFUPM-Sorter-Windows-Setup.exe");
        check("newer release found", newer != null && newer.version.equals("9.0.0") && newer.fileUrl.equals("https://github.com/x/setup.exe")
                && newer.pageUrl.endsWith("/v9.0.0"));
        check("same version is not offered", Updates.parse(rel, "9.0.0", null) == null && Updates.parse(rel, "10.0", null) == null);
        check("missing installer falls back to the release page", Updates.parse(rel, "1.0.0", "nothing.zip").fileUrl.endsWith("/v9.0.0"));
        check("drafts and pre-releases are ignored", Updates.parse(rel.replace("\"prerelease\":false", "\"prerelease\":true"), "1.0.0", null) == null
                && Updates.parse(rel.replace("\"draft\":false", "\"draft\":true"), "1.0.0", null) == null);
        check("only github.com links", Updates.parse(rel.replace("https://github.com/x/setup.exe", "https://evil.example/setup.exe"),
                "1.0.0", "KFUPM-Sorter-Windows-Setup.exe").fileUrl.startsWith("https://github.com/kal429/"));
        Updates.State st = new Updates.State();
        long now = System.currentTimeMillis();
        st.lastCheck = now - 3_600_000L;
        boolean notYet = !Updates.dueInBackground(st, now);
        st.lastCheck = now - 25L * 3_600_000L;
        check("background asks once a day", notYet && Updates.dueInBackground(st, now));
        // a local web server stands in for GitHub
        com.sun.net.httpserver.HttpServer srv = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        final String[] answer = {rel};
        final String[] agent = {""};
        srv.createContext("/latest", ex -> {
            agent[0] = ex.getRequestHeaders().getFirst("User-Agent");
            byte[] b = answer[0].getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        srv.start();
        System.setProperty("kfupmsorter.updateUrl", "http://127.0.0.1:" + srv.getAddress().getPort() + "/latest");
        Updates.Info got = Updates.check();
        Updates.State saved = Updates.State.load();
        check("check() over the network", got != null && got.version.equals("9.0.0") && saved.latest.equals("9.0.0")
                && saved.lastCheck > 0 && agent[0].startsWith("KFUPM-Sorter/"));
        answer[0] = rel.replace("v9.0.0", "v" + Main.VERSION);
        check("check() says up to date", Updates.check() == null && Updates.State.load().latest.isEmpty());
        // the API refuses (60 questions an hour per address): the public release page still answers
        String base = "http://127.0.0.1:" + srv.getAddress().getPort();
        srv.createContext("/refused", ex -> { ex.sendResponseHeaders(403, -1); ex.close(); });
        srv.createContext("/page", ex -> {
            ex.getResponseHeaders().add("Location", "https://github.com/kal429/kfupm-sorter-windows-mac/releases/tag/v9.1.0");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        System.setProperty("kfupmsorter.updateUrl", base + "/refused");
        System.setProperty("kfupmsorter.releasePage", base + "/page");
        Updates.Info viaPage = Updates.check();
        check("falls back to the release page", viaPage != null && viaPage.version.equals("9.1.0")
                && viaPage.pageUrl.endsWith("/releases/tag/v9.1.0"));
        Updates.Info mem = Updates.remembered();
        check("remembers the last answer", mem != null && mem.version.equals("9.1.0") && mem.pageUrl.equals(viaPage.pageUrl)
                && mem.fileUrl.equals(viaPage.fileUrl) && !Updates.dueForWindow());
        check("installer link from the tag", Updates.fromTagUrl("https://github.com/kal429/kfupm-sorter-windows-mac/releases/tag/v3.0.0",
                "1.0.0", "KFUPM-Sorter-Windows-Setup.exe").fileUrl
                .equals("https://github.com/kal429/kfupm-sorter-windows-mac/releases/download/v3.0.0/KFUPM-Sorter-Windows-Setup.exe")
                && Updates.fromTagUrl("https://evil.example/releases/tag/v3.0.0", "1.0.0", null) == null
                && Updates.fromTagUrl("https://github.com/kal429/kfupm-sorter-windows-mac/releases/tag/v1.0.0", "1.0.0", null) == null);
        srv.stop(0);
        System.setProperty("kfupmsorter.updateUrl", "http://127.0.0.1:9/nothing");
        System.setProperty("kfupmsorter.releasePage", "http://127.0.0.1:9/nothing");
        boolean threw = false;
        try { Updates.check(); } catch (Exception e) { threw = true; }
        check("no network is reported, not hidden", threw);
        System.clearProperty("kfupmsorter.updateUrl");
        System.clearProperty("kfupmsorter.releasePage");
        check("update setting defaults to on", new Prefs().checkUpdates);

        // ---- autostart file contents (written into a temp home)
        System.out.println();
        System.out.println(passes + " passed, " + fails + " failed");
        System.exit(fails);
    }
}
