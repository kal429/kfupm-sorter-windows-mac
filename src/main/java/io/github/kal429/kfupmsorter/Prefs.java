package io.github.kal429.kfupmsorter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** settings.json: interface language and whether automatic sorting is on. */
public final class Prefs {
    public String language = "en";
    public boolean auto = false;
    public boolean imported = false;    // the PowerShell edition's choices were already offered once

    public static Prefs load() {
        Prefs p = new Prefs();
        Path f = AppDirs.settingsFile();
        try {
            if (Files.exists(f)) {
                Map<String, Object> m = Json.obj(Json.parse(Files.readString(f, StandardCharsets.UTF_8)));
                String lang = Json.str(m.get("language"), "en");
                p.language = "ar".equals(lang) ? "ar" : "en";
                p.auto = Json.bool(m.get("auto"), false);
                p.imported = Json.bool(m.get("imported"), false);
            }
        } catch (Exception ignored) { }
        return p;
    }

    public void save() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("language", language);
        m.put("auto", auto);
        m.put("imported", imported);
        try { Files.writeString(AppDirs.settingsFile(), Json.write(m), StandardCharsets.UTF_8); } catch (Exception ignored) { }
    }
}
