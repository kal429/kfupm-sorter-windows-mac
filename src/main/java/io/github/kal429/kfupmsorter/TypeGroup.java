package io.github.kal429.kfupmsorter;

import java.util.ArrayList;
import java.util.List;

/** Folders for files that match no course: Documents, Images, Videos... */
public final class TypeGroup {
    public final String name;
    public final List<String> extensions;
    public final boolean onByDefault;

    private TypeGroup(String name, boolean onByDefault, String... extensions) {
        this.name = name;
        this.onByDefault = onByDefault;
        this.extensions = List.of(extensions);
    }

    public static final List<TypeGroup> ALL = List.of(
        new TypeGroup("Documents",   true,  "pdf", "docx", "doc", "pptx", "ppt", "xlsx", "xls", "csv", "txt", "md", "rtf", "odt", "pages", "key", "numbers"),
        new TypeGroup("Images",      true,  "png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "svg", "tiff"),
        new TypeGroup("Videos",      true,  "mp4", "mkv", "avi", "mov", "webm", "wmv", "flv", "m4v"),
        new TypeGroup("Audio",       true,  "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma"),
        new TypeGroup("Installers",  true,  "exe", "msi", "msix", "appx", "iso", "cab", "apk", "dmg", "pkg"),
        new TypeGroup("Archives",    true,  "zip", "rar", "7z", "tar", "gz", "bz2"),
        new TypeGroup("Code",        false, "py", "js", "ts", "java", "c", "cpp", "h", "cs", "json", "xml", "html", "css", "jar", "ipynb", "m", "r", "asm"),
        new TypeGroup("3D Printing", false, "stl", "3mf", "obj", "gcode", "step", "stp", "f3d"),
        new TypeGroup("eBooks",      false, "epub", "mobi", "azw3", "djvu")
    );

    public static List<String> defaults() {
        List<String> out = new ArrayList<>();
        for (TypeGroup g : ALL) if (g.onByDefault) out.add(g.name);
        return out;
    }
}
