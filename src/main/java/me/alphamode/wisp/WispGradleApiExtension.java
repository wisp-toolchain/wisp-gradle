package me.alphamode.wisp;

import me.alphamode.wisp.api.MinecraftJars;
import me.alphamode.wisp.gradle.WispLogger;
import me.alphamode.wisp.jar.minecraft.MinecraftJarProvider;
import me.alphamode.wisp.mappings.MappingProvider;
import me.alphamode.wisp.minecraft.MinecraftVersionManifest;
import me.alphamode.wisp.minecraft.RunConfig;
import me.alphamode.wisp.mod.ModProcessor;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.Nullable;

public interface WispGradleApiExtension {

    void minecraft(String version);

    default void enableDemo() {
        enableFeature("is_demo_user");
    }

    void enableFeature(String key);

    Property<String> getVersionManifestUrl();

    Property<String> getVersionsManifestUrl();

    Property<MinecraftVersionManifest> getVersion();

    Property<Boolean> getHasBundler();

    Property<MappingProvider> getMappingsProvider();

    Property<Boolean> clientOnly();

    Property<Boolean> serverOnly();

    Property<Boolean> includeGameJar();

    Property<String> getMinecraftVersion();

    RegularFileProperty getCacheDirectory();

    void runs(Action<NamedDomainObjectContainer<RunConfig>> action);

    NamedDomainObjectContainer<RunConfig> getRunConfigs();

    WispLogger getLogger();

    MinecraftJars getMinecraft();

    void setMinecraft(MinecraftJars minecraft);

    ModProcessor getModProcessor();

    MinecraftJarProvider getMinecraftProvider();

    void setMinecraftProvider(MinecraftJarProvider provider);

    static WispGradleApiExtension get(Project project) {
        return (WispGradleApiExtension) project.getExtensions().getByName("wisp");
    }
}