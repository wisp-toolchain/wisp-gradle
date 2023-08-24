package me.alphamode.wisp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alphamode.wisp.bundler.Bundler;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.ProGuardReader;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.Application;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.RunConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class WispGradle implements Plugin<Project> {
    public static final Logger LOGGER = LoggerFactory.getLogger(WispGradle.class);

    private static final Gson GSON = new Gson();

    private static final String ASSETS_URL = "https://resources.download.minecraft.net/";

    private Project project;

    private String mcVersion;

    public void setMcVersion(String version) {
        this.mcVersion = version;
    }

    @Override
    public void apply(Project project) {
        this.project = project;
        project.getExtensions().create(WispGradleExtension.class, "wisp", WispExtensionImpl.class, project, this);
        project.getPluginManager().apply("org.jetbrains.gradle.plugin.idea-ext");

        project.afterEvaluate(project1 -> {
            System.out.println("Loading minecraft environment for: " + mcVersion);
            try {
                JsonArray versions = GSON.fromJson(readString(new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")), JsonObject.class).getAsJsonArray("versions");

                JsonObject foundVersion = null;

                for (JsonElement ver : versions) {
                    JsonObject versionObj = ver.getAsJsonObject();

                    if (versionObj.get("id").getAsString().equals(mcVersion))
                        foundVersion = versionObj;
                }

                if (foundVersion == null)
                    throw new RuntimeException("Unable to find version: " + mcVersion);

                downloadGame(new URL(foundVersion.get("url").getAsString()), mcVersion);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        LOGGER.info("Loaded Wisp Gradle");
    }

    protected void downloadGame(URL address, String gameVersion) {
        JsonObject version = GSON.fromJson(readString(address), JsonObject.class);

        JsonArray libs = version.getAsJsonArray("libraries");

        var client = project.getRootDir().toPath().resolve("wisp/client/client-" + gameVersion + ".jar").toFile();
        var serverBundler = project.getRootDir().toPath().resolve("wisp/server/server-bundler-" + gameVersion + ".jar").toFile();
        var server = project.getRootDir().toPath().resolve("wisp/server/server-" + gameVersion + ".jar").toFile();
        Path mappedClient = project.getRootDir().toPath().resolve("wisp/client/client-mapped-" + gameVersion + ".jar");
        Path mappedServer = project.getRootDir().toPath().resolve("wisp/server/server-mapped" + gameVersion + ".jar");
        var clientMappingsProguard = project.getRootDir().toPath().resolve("wisp/client/mapping.txt");
        var serverMappingsProguard = project.getRootDir().toPath().resolve("wisp/server/mapping.txt");
        var clientMappings = project.getRootDir().toPath().resolve("wisp/client/mapping.tiny");
        var serverMappings = project.getRootDir().toPath().resolve("wisp/server/mapping.tiny");

        System.out.println("Downloading client");
        if (!client.exists()) {
            try {
                ReadableByteChannel clientJarChannel = Channels.newChannel(new URL(version.getAsJsonObject("downloads").get("client").getAsJsonObject().get("url").getAsString()).openStream());
                client.getParentFile().mkdirs();
                try (FileOutputStream fileOutputStream = new FileOutputStream(client)) {

                    fileOutputStream.getChannel()
                            .transferFrom(clientJarChannel, 0, Long.MAX_VALUE);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to download the game");
            }
        }

        System.out.println("Downloading server");
        if (!mappedServer.toFile().exists()) {
            try {
                ReadableByteChannel serverJarChannel = Channels.newChannel(new URL(version.getAsJsonObject("downloads").get("server").getAsJsonObject().get("url").getAsString()).openStream());
                serverBundler.getParentFile().mkdirs();
                try (FileOutputStream fileOutputStream = new FileOutputStream(serverBundler)) {

                    fileOutputStream.getChannel()
                            .transferFrom(serverJarChannel, 0, Long.MAX_VALUE);
                }
                Files.write(serverMappingsProguard, new URL(version.getAsJsonObject("downloads").get("server_mappings").getAsJsonObject().get("url").getAsString()).openStream().readAllBytes());
                try (Writer writer = new StringWriter()) {
                    MappingWriter mappingTree = new Tiny2Writer(writer, false);
                    MappingSourceNsSwitch sourceNsSwitch = new MappingSourceNsSwitch(mappingTree, "named", true);
                    AddConstructorMappingVisitor constructorMappingVisitor = new AddConstructorMappingVisitor(sourceNsSwitch);
                    ProGuardReader.read(new FileReader(serverMappingsProguard.toFile()), "named", "official", constructorMappingVisitor);
                    Files.write(serverMappings, writer.toString().getBytes(StandardCharsets.UTF_8));
                }

                try (Bundler bundler = new Bundler(serverBundler.toPath())) {
                    Files.copy(bundler.getBundlerFs().getPath(bundler.getServerJar().path()), server.toPath());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> classPaths = new ArrayList<>();

        System.out.println("Downloading libraries");
        for (JsonElement lib : libs) {
            JsonObject artifact = lib.getAsJsonObject().get("downloads").getAsJsonObject().get("artifact").getAsJsonObject();
            var path = project.getRootDir().toPath().resolve("wisp/libs/" + artifact.get("path").getAsString()).toFile();
            if (!path.exists()) {
                try {
                    ReadableByteChannel libChannel = Channels.newChannel(new URL(artifact.get("url").getAsString()).openStream());
                    path.getParentFile().mkdirs();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(path)) {

                        fileOutputStream.getChannel()
                                .transferFrom(libChannel, 0, Long.MAX_VALUE);
                    }
                } catch (IOException e) {
                    System.out.println("Failed to download:" + lib.getAsJsonObject().get("name").getAsString());
                }
            }
            classPaths.add(path.getAbsolutePath());
            project.getDependencies().add("implementation", project.files(path));
        }

        try {
            Files.write(clientMappingsProguard, new URL(version.getAsJsonObject("downloads").get("client_mappings").getAsJsonObject().get("url").getAsString()).openStream().readAllBytes());

            try (Writer writer = new StringWriter()) {
                MappingWriter mappingTree = new Tiny2Writer(writer, false);
                MappingSourceNsSwitch sourceNsSwitch = new MappingSourceNsSwitch(mappingTree, "named", true);
                AddConstructorMappingVisitor constructorMappingVisitor = new AddConstructorMappingVisitor(sourceNsSwitch);
                ProGuardReader.read(new FileReader(clientMappingsProguard.toFile()), "named", "official", constructorMappingVisitor);
                Files.write(clientMappings, writer.toString().getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var clientRemapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(clientMappings, "official", "named"))
                .rebuildSourceFilenames(true)
                .build();

        System.out.println("Remapping client");
        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(mappedClient)
                // force jar despite the .tmp extension
                .assumeArchive(true)
                .build()) {

            for (String gameLib : classPaths) {
                clientRemapper.readClassPath(Path.of(gameLib));
            }

            outputConsumer.addNonClassFiles(client.toPath(), NonClassCopyMode.FIX_META_INF, clientRemapper);
            clientRemapper.readInputs(client.toPath());

            clientRemapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            clientRemapper.finish();
            project.getDependencies().add("implementation", project.files(mappedClient));
        }

        System.out.println("Remapping server");
        var serverRemapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(serverMappings, "official", "named"))
                .rebuildSourceFilenames(true)
                .build();

        try {

            Path tempMapped = project.getRootDir().toPath().resolve("wisp/server/server-" + mcVersion + "-mapped.jar");
            Path unMapped = project.getRootDir().toPath().resolve("wisp/server/server-" + mcVersion + "-unmapped.jar");
            try {
                Files.copy(serverBundler.toPath(), unMapped);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(tempMapped)
                    // force jar despite the .tmp extension
                    .assumeArchive(true)
                    .build()) {

                for (String gameLib : classPaths) {
                    serverRemapper.readClassPath(Path.of(gameLib));
                }

                outputConsumer.addNonClassFiles(unMapped, NonClassCopyMode.FIX_META_INF, serverRemapper);
                serverRemapper.readInputs(unMapped);

                serverRemapper.apply(outputConsumer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                serverRemapper.finish();
                Files.copy(tempMapped, mappedServer);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        project.getDependencies().add("implementation", project.files(serverBundler.toPath()));

        JsonObject arguments = version.getAsJsonObject("arguments");
        JsonArray gameArgs = arguments.getAsJsonArray("game");
        JsonArray jvmArgs = arguments.getAsJsonArray("jvm");

        StringBuilder args = new StringBuilder();
        for (Iterator<JsonElement> it = gameArgs.iterator(); it.hasNext(); ) {
            JsonElement arg = it.next();
            if (arg.isJsonObject())
                continue;
            args.append(arg.getAsString());
            if (it.hasNext())
                args.append(" ");
        }

        var assetDir = project.getRootDir().toPath().resolve("wisp/assets");

        String mcArgs = args.toString().replace("${assets_root}", project.getRootDir().toPath().resolve("wisp/assets").toString()).replace("${assets_index_name}", version.get("assets").getAsString()).replace("${version_type}", version.get("type").getAsString());

        JsonObject assetIndex = version.getAsJsonObject("assetIndex");

        File assetFile = assetDir.resolve("indexes/" + assetIndex.get("id").getAsString() + ".json").toFile();
        if (!assetFile.exists()) {
            try {
                ReadableByteChannel assetIndexChannel = Channels.newChannel(new URL(assetIndex.get("url").getAsString()).openStream());

                assetFile.getParentFile().mkdirs();
                try (FileOutputStream fileOutputStream = new FileOutputStream(assetFile)) {

                    fileOutputStream.getChannel()
                            .transferFrom(assetIndexChannel, 0, Long.MAX_VALUE);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to download asset index");
            }
        }

        Path objectsDir = assetDir.resolve("objects");

        try {
            JsonObject objects = GSON.fromJson(new FileReader(assetFile), JsonObject.class).getAsJsonObject("objects");
            objects.asMap().forEach((s, jsonElement) -> {
                JsonObject asset = jsonElement.getAsJsonObject();

                String assetHash = asset.get("hash").getAsString();
                var assetFolder = objectsDir.resolve(assetHash.substring(0, 2) + "/" + assetHash).toFile();
                if (!assetFolder.exists()) {
                    try {
                        ReadableByteChannel assetIndexChannel = Channels.newChannel(new URL(ASSETS_URL + assetHash.substring(0, 2) + "/" + assetHash).openStream());

                        assetFolder.getParentFile().mkdirs();
                        try (FileOutputStream fileOutputStream = new FileOutputStream(assetFolder)) {

                            fileOutputStream.getChannel()
                                    .transferFrom(assetIndexChannel, 0, Long.MAX_VALUE);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to download asset index");
                    }
                }
            });
        } catch (FileNotFoundException e) {

        }

        Application clientConfig = new Application("Minecraft Client", project);

        clientConfig.setMainClass("me.alphamode.wisp.loader.Main");
        clientConfig.setModuleName(String.format("%s.main", project.getName()));
        clientConfig.setProgramParameters(mcArgs);
//        clientConfig.setJvmArgs(jvmArgs); TODO
        clientConfig.setWorkingDirectory("run/");

        Application serverConfig = new Application("Minecraft Server", project);
        serverConfig.setMainClass("net.minecraft.bundler.Main");
        serverConfig.setModuleName(String.format("%s.server", project.getName()));
        clientConfig.setWorkingDirectory("run/");

        IdeaModel ideaModel = ((IdeaModel) project.getExtensions().findByName("idea"));

        if (ideaModel == null) return;

        if (ideaModel.getProject() != null) {
            ProjectSettings settings = ((ExtensionAware) ideaModel.getProject()).getExtensions().getByType(ProjectSettings.class);
            NamedDomainObjectContainer<RunConfiguration> runConfigurations = (NamedDomainObjectContainer<RunConfiguration>)
                    ((ExtensionAware) settings).getExtensions().getByName("runConfigurations");
            runConfigurations.add(clientConfig);
            runConfigurations.add(serverConfig);
        }
    }

    private static String readString(URL address) {
        try {
            return new String(address.openStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}