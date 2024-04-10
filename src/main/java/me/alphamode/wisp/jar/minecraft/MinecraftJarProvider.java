package me.alphamode.wisp.jar.minecraft;

import org.gradle.api.Project;

public interface MinecraftJarProvider {
    void provide(Project project);
}
