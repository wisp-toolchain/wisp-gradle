package me.alphamode.wisp;

import org.gradle.api.Project;

public class WispExtensionImpl implements WispGradleExtension {
    private final Project project;
    private final WispGradle gradle;

    public WispExtensionImpl(Project project, WispGradle gradle) {
        this.project = project;
        this.gradle = gradle;
    }

    @Override
    public void minecraft(String version) {
        this.gradle.setMcVersion(version);
    }
}