package me.alphamode.wisp;

import org.gradle.api.Project;

import java.nio.file.Path;

public class WispExtensionImpl implements WispGradleExtension {
    private final WispGradleApiExtension extension;
    private final Project project;

    public WispExtensionImpl(Project project, WispGradleApiExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    @Override
    public Path getCache(String path) {
        return project.getRootDir().toPath().resolve("wisp").resolve(path);
    }

    @Override
    public String getMcVersion() {
        return extension.getMinecraftVersion().get();
    }

    @Override
    public boolean hasIntegratedServer() {
        return true; // TODO
    }
}
