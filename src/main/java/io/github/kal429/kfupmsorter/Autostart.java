package io.github.kal429.kfupmsorter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Starts the background sorter when you log in, without administrator rights:
 *   Windows: a tiny launcher script in your Startup folder (no console window)
 *   macOS:   a LaunchAgent in ~/Library/LaunchAgents
 *   Linux:   a .desktop file in ~/.config/autostart
 */
public final class Autostart {
    private Autostart() {}

    static final String MAC_LABEL = "io.github.kal429.kfupmsorter";

    /** The command that starts this program again with --background. */
    public static List<String> backgroundCommand() {
        List<String> cmd = new ArrayList<>();
        Optional<String> self = ProcessHandle.current().info().command();
        String exe = self.orElse("");
        String low = exe.toLowerCase();
        boolean plainJava = low.endsWith("java") || low.endsWith("java.exe") || low.endsWith("javaw.exe") || exe.isEmpty();
        if (plainJava) {
            // started as "java -jar ..." (development): run the same class path again
            String javaBin = Paths.get(System.getProperty("java.home"), "bin",
                    AppDirs.OS_TYPE == AppDirs.OS.WINDOWS ? "javaw.exe" : "java").toString();
            cmd.add(javaBin);
            for (String prop : new String[]{"kfupmsorter.data", "user.home"}) {   // keep test settings
                if (System.getProperty(prop) != null && (prop.equals("kfupmsorter.data") || System.getProperty("kfupmsorter.data") != null)) {
                    cmd.add("-D" + prop + "=" + System.getProperty(prop));
                }
            }
            cmd.add("-cp");
            cmd.add(System.getProperty("java.class.path"));
            cmd.add(Main.class.getName());
        } else {
            cmd.add(exe);   // the app's own launcher (KFUPM Sorter Desktop.exe / .app/Contents/MacOS/...)
        }
        cmd.add("--background");
        return cmd;
    }

    static Path windowsStartupFile() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Paths.get(appData) : AppDirs.home().resolve("AppData").resolve("Roaming");
        return base.resolve("Microsoft").resolve("Windows").resolve("Start Menu").resolve("Programs")
                   .resolve("Startup").resolve(AppDirs.APP_NAME + ".vbs");
    }

    static Path macAgentFile() {
        return AppDirs.home().resolve("Library").resolve("LaunchAgents").resolve(MAC_LABEL + ".plist");
    }

    static Path linuxAutostartFile() {
        return AppDirs.home().resolve(".config").resolve("autostart").resolve("kfupm-sorter-desktop.desktop");
    }

    private static Path file() {
        switch (AppDirs.OS_TYPE) {
            case WINDOWS: return windowsStartupFile();
            case MAC:     return macAgentFile();
            default:      return linuxAutostartFile();
        }
    }

    public static boolean isEnabled() { return Files.exists(file()); }

    private static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void enable() throws IOException {
        List<String> cmd = backgroundCommand();
        Path f = file();
        Files.createDirectories(f.getParent());
        String content;
        switch (AppDirs.OS_TYPE) {
            case WINDOWS: {
                // VBScript: Run "cmd", 0 = hidden window, False = don't wait
                StringBuilder line = new StringBuilder();
                for (String part : cmd) {
                    if (line.length() > 0) line.append(' ');
                    line.append(part.startsWith("--") ? part : "\"\"" + part + "\"\"");
                }
                content = "' Starts KFUPM Sorter Desktop in the background when you sign in.\r\n"
                        + "CreateObject(\"WScript.Shell\").Run \"" + line + "\", 0, False\r\n";
                break;
            }
            case MAC: {
                StringBuilder args = new StringBuilder();
                for (String part : cmd) args.append("    <string>").append(xml(part)).append("</string>\n");
                content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                        + "<plist version=\"1.0\">\n<dict>\n"
                        + "  <key>Label</key>\n  <string>" + MAC_LABEL + "</string>\n"
                        + "  <key>ProgramArguments</key>\n  <array>\n" + args + "  </array>\n"
                        + "  <key>RunAtLoad</key>\n  <true/>\n"
                        + "  <key>ProcessType</key>\n  <string>Background</string>\n"
                        + "</dict>\n</plist>\n";
                break;
            }
            default: {
                StringBuilder exec = new StringBuilder();
                for (String part : cmd) exec.append(exec.length() > 0 ? " " : "").append('"').append(part).append('"');
                content = "[Desktop Entry]\nType=Application\nName=" + AppDirs.APP_NAME + "\nExec=" + exec
                        + "\nX-GNOME-Autostart-enabled=true\nNoDisplay=true\n";
            }
        }
        Files.writeString(f, content, StandardCharsets.UTF_8);
    }

    public static void disable() throws IOException {
        Files.deleteIfExists(file());
    }

    /** Starts the background sorter right now (it keeps running after the window closes). */
    public static void startNow() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(backgroundCommand());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.start();
    }
}
