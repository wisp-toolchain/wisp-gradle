package me.alphamode.wisp.minecraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alphamode.wisp.WispGradle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record MinecraftVersionManifest(String version, String type, Map<String, Download> downloads, List<Library> libraries, String mainClass, String assets, AssetIndex assetIndex) {
    public static MinecraftVersionManifest fromJson(JsonObject manifest) {
        String version = manifest.get("id").getAsString();
        String type = manifest.get("type").getAsString();
        Map<String, Download> downloads = new HashMap<>();
        manifest.getAsJsonObject("downloads").entrySet().forEach(entry -> {
            downloads.put(entry.getKey(), Download.fromJson(entry.getValue().getAsJsonObject(), false));
        });

        List<Library> libraries = new ArrayList<>();
        for (JsonElement lib : manifest.getAsJsonArray("libraries")) {
            System.out.println(lib);
            var library = Library.fromJson(lib.getAsJsonObject());
            if (LibraryProcessor.process(library))
                libraries.add(library);
        }

        String mainClass = manifest.get("mainClass").getAsString();
        String assets = manifest.get("assets").getAsString();

        Download index = Download.fromJson(manifest.getAsJsonObject("assetIndex"), false);

        return new MinecraftVersionManifest(version, type, downloads, libraries, mainClass, assets, AssetIndex.fromJson(WispGradle.GSON.fromJson(index.readString(), JsonObject.class)));
    }

    public Download getDownload(String key) {
        return downloads.get(key);
    }
}