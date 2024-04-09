package me.alphamode.wisp.api;

import me.alphamode.wisp.env.Environment;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public record MinecraftJars(List<MinecraftJar> jars) {

    public Path getClient() {
        for (MinecraftJar jar : jars) {
            if (jar.environment == Environment.CLIENT)
                return jar.jar;
        }
        throw new RuntimeException("No client jar available!");
    }

    public Path getServer() {
        for (MinecraftJar jar : jars) {
            if (jar.environment == Environment.SERVER)
                return jar.jar;
        }
        throw new RuntimeException("No server jar available!");
    }

    public record MinecraftJar(@Nullable Environment environment, Path jar) {}
}
