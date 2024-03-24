package me.alphamode.wisp;

import me.alphamode.wisp.util.WispConstants;
import org.gradle.api.Project;
import org.gradle.wrapper.Download;
import org.gradle.wrapper.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class Downloader {
    private final Download download = new Download(new Logger(true), "wisp", "1.0", 60 * 1000);
    private final Project project;
    public Downloader(Project project) {
        this.project = project;
    }

    public Path downloadArtifact(me.alphamode.wisp.minecraft.Download artifact, Path folder) {
        var path = artifact.path() != null ? folder.resolve(artifact.path()).toFile() : folder.toFile();
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
        if (artifact.isNative())
            project.getDependencies().add(WispConstants.MINECRAFT_NATIVES, project.files(path));
        else
            project.getDependencies().add(WispConstants.MINECRAFT_LIBRARIES, project.files(path));
        return path.toPath();
    }

    public Download getDownload() {
        return download;
    }
}
