package me.alphamode.wisp.minecraft;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public record AssetIndex(Map<String, Asset> assets, boolean mapToResources) {
    public static AssetIndex fromJson(JsonObject assetIndex) {
        Map<String, Asset> assets = new HashMap<>();
        assetIndex.getAsJsonObject("objects").entrySet().forEach(entry -> {
            var asset = entry.getValue().getAsJsonObject();
            assets.put(entry.getKey(), new Asset(asset.get("hash").getAsString(), asset.get("size").getAsLong()));
        });

        return new AssetIndex(assets, assetIndex.has("map_to_resources") && assetIndex.get("map_to_resources").getAsBoolean());
    }

    public record Asset(String hash, long size) {}
}
