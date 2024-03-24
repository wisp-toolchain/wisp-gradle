package me.alphamode.wisp;

import me.alphamode.wisp.api.MinecraftJars;
import me.alphamode.wisp.gradle.WispLogger;
import me.alphamode.wisp.mappings.MappingProvider;
import me.alphamode.wisp.mappings.MojangMappingsProvider;
import me.alphamode.wisp.minecraft.MinecraftVersionManifest;
import me.alphamode.wisp.minecraft.RunConfig;
import me.alphamode.wisp.mod.ModProcessor;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WispExtensionApiImpl implements WispGradleApiExtension {

    private final Project project;
    private final WispGradle gradle;

    private final Property<String> minecraftVersion;
    private final Property<String> versionManifest;
    private final Property<String> versionsManifest;
    private final Property<MinecraftVersionManifest> version;
    private final Property<Boolean> hasBundler;
    private final Property<MappingProvider> mappingProvider;
    private final Property<Boolean> clientOnly, serverOnly, noGameJar;
    private final RegularFileProperty cacheDir;
    private final NamedDomainObjectContainer<RunConfig> runConfigs;
    private final WispLogger logger;
    private final ModProcessor processor;

    private MinecraftJars minecraftJar;


    public WispExtensionApiImpl(Project project, WispGradle gradle) {
        this.project = project;
        this.gradle = gradle;
        this.minecraftVersion = project.getObjects().property(String.class);
        this.versionManifest = project.getObjects().property(String.class);
        this.versionsManifest = project.getObjects().property(String.class).convention("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
        this.hasBundler = project.getObjects().property(Boolean.class).convention(true);
        this.mappingProvider = project.getObjects().property(MappingProvider.class).convention(new MojangMappingsProvider(project));
        this.clientOnly = project.getObjects().property(Boolean.class).convention(false);
        this.serverOnly = project.getObjects().property(Boolean.class).convention(false);
        this.noGameJar = project.getObjects().property(Boolean.class).convention(true);
        this.version = project.getObjects().property(MinecraftVersionManifest.class);
        this.cacheDir = project.getObjects().fileProperty();
        this.runConfigs = project.container(RunConfig.class, RunConfig::new);
        if (!cacheDir.isPresent())
            cacheDir.set(new File(project.getGradle().getGradleUserHomeDir(), "caches/wisp-gradle"));
        this.logger = new WispLogger(project);
        this.processor = new ModProcessor();
    }

    @Override
    public void minecraft(String version) {
        this.minecraftVersion.set(version);
    }

    @Override
    public Property<String> getMinecraftVersion() {
        return this.minecraftVersion;
    }

    @Override
    public void enableFeature(String key) {
//        this.gradle.getPredicateArgs().put(key, true);
    }

    @Override
    public Property<String> getVersionManifestUrl() {
        return this.versionManifest;
    }

    @Override
    public Property<String> getVersionsManifestUrl() {
        return this.versionsManifest;
    }

    @Override
    public Property<MinecraftVersionManifest> getVersion() {
        return this.version;
    }

    @Override
    public Property<Boolean> getHasBundler() {
        return this.hasBundler;
    }

    @Override
    public Property<MappingProvider> getMappingsProvider() {
        return this.mappingProvider;
    }

    @Override
    public Property<Boolean> clientOnly() {
        return this.clientOnly;
    }

    @Override
    public Property<Boolean> serverOnly() {
        return this.serverOnly;
    }

    public Property<Boolean> includeGameJar() {
        return this.noGameJar;
    }

    @Override
    public RegularFileProperty getCacheDirectory() {
        return this.cacheDir;
    }

    @Override
    public WispLogger getLogger() {
        return this.logger;
    }

    @Override
    public ModProcessor getModProcessor() {
        return this.processor;
    }

    @Override
    public MinecraftJars getMinecraft() {
        return this.minecraftJar;
    }

    @Override
    public void setMinecraft(MinecraftJars minecraft) {
        this.minecraftJar = minecraft;
    }

    @Override
    public void runs(Action<NamedDomainObjectContainer<RunConfig>> action) {
        action.execute(this.runConfigs);
    }

    @Override
    public NamedDomainObjectContainer<RunConfig> getRunConfigs() {
        return this.runConfigs;
    }

    public static String readString(URL address) {
        try {
            return new String(address.openStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}