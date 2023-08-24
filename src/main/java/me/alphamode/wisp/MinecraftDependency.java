package me.alphamode.wisp;

import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

public class MinecraftDependency implements Dependency {

    private final String version;

    public MinecraftDependency(String version) {
        this.version = version;
    }

    @Nullable
    @Override
    public String getGroup() {
        return null;
    }

    @Override
    public String getName() {
        return "minecraft";
    }

    @Nullable
    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public boolean contentEquals(Dependency dependency) {
        return false;
    }

    @Override
    public Dependency copy() {
        return null;
    }

    @Nullable
    @Override
    public String getReason() {
        return null;
    }

    @Override
    public void because(@Nullable String reason) {

    }
}
