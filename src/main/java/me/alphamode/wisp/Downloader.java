package me.alphamode.wisp;

import me.alphamode.wisp.util.WispConstants;
import org.gradle.api.Project;
import org.gradle.wrapper.Download;
import org.gradle.wrapper.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class Downloader {
    private final Download download = new Download(new Logger(true), "wisp", "1.0", 60 * 1000);
    private final Project project;
    public Downloader(Project project) {
        this.project = project;
    }

    public Path downloadArtifact(me.alphamode.wisp.minecraft.Download artifact, File path) {
        if (!path.exists()) {
            try {
                download.download(URI.create(artifact.url()), path);
            } catch (IOException e) {
                WispGradleApiExtension.get(project).getLogger().error("Failed to download:" + path);
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return path.toPath();
    }

    public Download getDownload() {
        return download;
    }
}
