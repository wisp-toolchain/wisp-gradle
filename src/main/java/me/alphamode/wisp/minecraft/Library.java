package me.alphamode.wisp.minecraft;

import com.google.gson.JsonObject;
import org.gradle.internal.os.OperatingSystem;

public record Library(String name, Download download) {

    public static Library fromJson(JsonObject lib) {
        org.gradle.internal.os.OperatingSystem os = OperatingSystem.current();
        String nativeType = "";
        if (lib.has("natives")) {
            var natives = lib.getAsJsonObject("natives");

            if (os.isLinux())
                nativeType = natives.get("linux").getAsString();
            else if (os.isMacOsX()) {
                nativeType = natives.get("osx").getAsString();
            } else {
                // Just assume everything else is windows
                nativeType = natives.get("windows").getAsString();
            }
        }
        JsonObject downloads = lib.getAsJsonObject("downloads");
        Download download = null;

        if (downloads.has("classifiers")) {
            download = Download.fromJson(downloads.getAsJsonObject("classifiers").getAsJsonObject(nativeType), true);
        }

        if (downloads.has("artifact")) {
            download = Download.fromJson(downloads.getAsJsonObject("artifact"), false);
        }

        if (download == null)
            throw new RuntimeException("Download not found: " + lib);

        return new Library(lib.get("name").getAsString(), download);
    }
}
