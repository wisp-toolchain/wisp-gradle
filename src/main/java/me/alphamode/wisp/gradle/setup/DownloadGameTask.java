package me.alphamode.wisp.gradle.setup;

import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.bundler.Bundler;
import me.alphamode.wisp.minecraft.MinecraftVersionManifest;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

public class DownloadGameTask extends WispTask {
    public static final String DOWNLOAD_GAME_TASK = "downloadGame";

    private final File clientOutput, serverOutput, serverBlunderOutput;

    public File getClientOutput() {
        return this.clientOutput;
    }

    public File getServerOutput() {
        return this.serverOutput;
    }

    public File getServerBlunderOutput() {
        return this.serverBlunderOutput;
    }

    public DownloadGameTask(Project project) {
        super(project);
        WispGradleExtension wisp = WispGradleExtension.get(this.project);
        this.clientOutput = wisp.getMcCache("client/client-" + wisp.getMcVersion() + ".jar").toFile();
        this.serverOutput = wisp.getMcCache("server/server-" + wisp.getMcVersion() + ".jar").toFile();
        this.serverBlunderOutput = wisp.getMcCache("server/server-bundler-" + wisp.getMcVersion() + ".jar").toFile();
    }

    @Override
    public void run() {
        WispGradleApiExtension wisp = WispGradleApiExtension.get(this.project);
        MinecraftVersionManifest manifest = wisp.getVersion().get();
        boolean includeGameJar = wisp.includeGameJar().get();
        var client = getClientOutput();
        if (wisp.clientOnly().get() && wisp.serverOnly().get()) {
            throw new RuntimeException("Can't have both client and server only!");
        }
        if (!wisp.serverOnly().get() && includeGameJar) {
            if (!client.exists()) {
                wisp.getLogger().log("Downloading client");
                try {
                    downloader.getDownload().download(URI.create(manifest.getDownload("client").url()), client);
                } catch (Exception e) {
                    wisp.getLogger().error("Failed to download client");
                    e.printStackTrace();
                }
            }
        }

        var server = getServerOutput();
        var serverBundler = getServerBlunderOutput();
        if (!wisp.clientOnly().get() && includeGameJar) {
            if (!server.exists()) {
                wisp.getLogger().log("Downloading server");
                boolean hasBundler = wisp.getHasBundler().get();
                try {
                    downloader.getDownload().download(URI.create(manifest.getDownload("server").url()), hasBundler ? serverBundler : server);
                } catch (Exception e) {
                    wisp.getLogger().error("Failed to download server");
                    e.printStackTrace();
                }

                if (hasBundler) {
                    wisp.getLogger().log("Extracting server jar from bundler");
                    try (Bundler bundler = new Bundler(serverBundler.toPath())) {
                        Files.copy(bundler.getBundlerFs().getPath("/META-INF/versions/" + bundler.getServerJar().path()), server.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}