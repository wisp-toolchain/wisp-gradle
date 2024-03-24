package me.alphamode.wisp;

import org.gradle.api.Project;

import java.nio.file.Path;

public interface WispGradleExtension {
    static WispGradleExtension get(Project project) {
        return (WispGradleExtension) project.getExtensions().getByName("wisp-impl");
    }

    Path getCache(String path);

    default Path getMcCache(String path) {
        return getCache(getMcVersion()).resolve(path);
    }

    String getMcVersion();
}
