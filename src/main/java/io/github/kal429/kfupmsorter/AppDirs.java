package io.github.kal429.kfupmsorter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Where the app keeps its files on each operating system. */
public final class AppDirs {
    private AppDirs() {}

    public static final String APP_NAME = "KFUPM Sorter Desktop";

    public enum OS { WINDOWS, MAC, OTHER }

    public static final OS OS_TYPE;
    static {
        String n = System.getProperty("os.name", "").toLowerCase();
        OS_TYPE = n.contains("win") ? OS.WINDOWS : (n.contains("mac") || n.contains("darwin")) ? OS.MAC : OS.OTHER;
    }

    public static Path home() { return Paths.get(System.getProperty("user.home")); }

    /** Settings, log and the downloaded catalog. Never inside the program folder. */
    public static Path dataDir() {
        String override = System.getProperty("kfupmsorter.data");   // used by the tests
        Path p;
        if (override != null) {
            p = Paths.get(override);
        } else if (OS_TYPE == OS.WINDOWS) {
            String appData = System.getenv("APPDATA");
            p = (appData != null ? Paths.get(appData) : home().resolve("AppData").resolve("Roaming")).resolve(APP_NAME);
        } else if (OS_TYPE == OS.MAC) {
            p = home().resolve("Library").resolve("Application Support").resolve(APP_NAME);
        } else {
            p = home().resolve(".config").resolve("kfupm-sorter-desktop");
        }
        try { Files.createDirectories(p); } catch (IOException ignored) { }
        return p;
    }

    public static Path defaultWatchFolder() { return home().resolve("Downloads"); }

    public static Path rulesFile()    { return dataDir().resolve("rules.json"); }
    public static Path settingsFile() { return dataDir().resolve("settings.json"); }
    public static Path logFile()      { return dataDir().resolve("sorter.log"); }
    public static Path lastRunFile()  { return dataDir().resolve("lastrun.txt"); }
    public static Path catalogFile()  { return dataDir().resolve("courses.json"); }

    /** rules.json of the Windows PowerShell edition, imported on first start. */
    public static Path powershellEditionRules() {
        String appData = System.getenv("APPDATA");
        if (OS_TYPE != OS.WINDOWS || appData == null) return null;
        return Paths.get(appData, "KFUPM Sorter", "rules.json");
    }

    /** Heartbeat of the PowerShell edition, to warn when both editions sort the same folder. */
    public static Path powershellEditionHeartbeat() {
        String appData = System.getenv("APPDATA");
        if (OS_TYPE != OS.WINDOWS || appData == null) return null;
        return Paths.get(appData, "KFUPM Sorter", "lastrun.txt");
    }
}
