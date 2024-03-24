package me.alphamode.wisp.minecraft;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public record Download(@Nullable String path, String sha1, long size, String url, boolean isNative) {
    public static Download fromJson(JsonObject artifact, boolean isNative) {
        return new Download(artifact.has("path") ? artifact.get("path").getAsString() : null, artifact.get("sha1").getAsString(), artifact.get("size").getAsLong(), artifact.get("url").getAsString(), isNative);
    }

    public String readString() {
        try {
            return new String(new URL(url()).openStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
