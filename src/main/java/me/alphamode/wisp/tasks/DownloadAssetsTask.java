package me.alphamode.wisp.tasks;

import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.minecraft.AssetIndex;
import org.gradle.api.tasks.Sync;

import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

public class DownloadAssetsTask extends Sync {
    private static final String ASSETS_URL = "https://resources.download.minecraft.net/";

    @Inject
    public DownloadAssetsTask() {
        WispGradleApiExtension wispApi = WispGradleApiExtension.get(getProject());
        WispGradleExtension wisp = WispGradleExtension.get(getProject());
        AssetIndex assetIndex = wispApi.getVersion().get().assetIndex();
        var assetDir = assetIndex.mapToResources() ? getProject().getProjectDir().toPath().resolve("run") : wisp.getMcCache("assets");
        Path objectsDir = assetIndex.mapToResources() ? assetDir.resolve("resources") : assetDir.resolve("objects");

        System.out.println("Downloading assets");
        wispApi.getLogger().log("Downloading assets");

        wispApi.getLogger().push();

        assetIndex.assets().forEach((key, asset) -> {
            String assetHash = asset.hash();
            var assetFolder = objectsDir.resolve(assetIndex.mapToResources() ? key : assetHash.substring(0, 2) + "/" + assetHash).toFile();
            if (!assetFolder.exists()) {
                wispApi.getLogger().log("Downloading: " + asset);
                try {
                    ReadableByteChannel assetIndexChannel = Channels.newChannel(new URL(ASSETS_URL + assetHash.substring(0, 2) + "/" + assetHash).openStream());

                    assetFolder.getParentFile().mkdirs();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(assetFolder)) {

                        fileOutputStream.getChannel()
                                .transferFrom(assetIndexChannel, 0, Long.MAX_VALUE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });

        wispApi.getLogger().pop();
        wispApi.getLogger().log("Finished downloading assets");
    }
}
