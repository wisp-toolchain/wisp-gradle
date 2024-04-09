package me.alphamode.wisp;

import com.google.gson.*;
import me.alphamode.wisp.gradle.setup.*;
import me.alphamode.wisp.minecraft.MinecraftVersionManifest;
import me.alphamode.wisp.minecraft.RunConfig;
import me.alphamode.wisp.tasks.*;
import me.alphamode.wisp.util.WispConstants;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.Application;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.RunConfiguration;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

public class WispGradle implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().registerTypeHierarchyAdapter(JvmArgs.class, new JvmArgs.Deserializer()).create();


    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;
        WispExtensionApiImpl wispApi = (WispExtensionApiImpl) project.getExtensions().create(WispGradleApiExtension.class, "wisp", WispExtensionApiImpl.class, project, this);
        WispGradleExtension wisp = project.getExtensions().create(WispGradleExtension.class, "wisp-impl", WispExtensionImpl.class, wispApi);
        var logger = wispApi.getLogger();
        project.getPluginManager().apply("org.jetbrains.gradle.plugin.idea-ext");

        project.getConfigurations().register(WispConstants.MINECRAFT_NATIVES, files -> files.setCanBeConsumed(true));
//        project.getConfigurations().register(WispConstants.MINECRAFT_LIBRARIES, files -> {
//            files.setTransitive(false);
//            files.setCanBeConsumed(false);
//            files.setCanBeResolved(true);
//        });
//        project.getConfigurations().register(WispConstants.MINECRAFT, files -> {
//            files.setTransitive(false);
//            files.setCanBeConsumed(false);
//            files.setCanBeResolved(false);
//        });

        project.afterEvaluate(proj -> {
            var runConfigs = wispApi.getRunConfigs();
            boolean clientOnly = wispApi.clientOnly().get();
            boolean serverOnly = wispApi.serverOnly().get();

            RunConfig clientConfig = new RunConfig("client");
            clientConfig.client();
            clientConfig.sourceSet(proj.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME));

            RunConfig serverConfig = new RunConfig("server");
            serverConfig.server();
            serverConfig.sourceSet(proj.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME));

            if (!serverOnly)
                runConfigs.add(clientConfig);
            if (!clientOnly)
                runConfigs.add(serverConfig);

            wispApi.getModProcessor().calculateMods(project);

            logger.log("Loading minecraft environment for: " + wispApi.getMinecraftVersion().get());
            logger.log("Working from: " + this.project.getProjectDir().toPath().resolve("wisp"));

            String mcVersion = wispApi.getMinecraftVersion().get();

            try {
                if (!wispApi.getVersionManifestUrl().isPresent()) {
                    JsonArray versions = GSON.fromJson(WispExtensionApiImpl.readString(new URL(wispApi.getVersionsManifestUrl().get())), JsonObject.class).getAsJsonArray("versions");

                    JsonObject foundVersion = null;

                    for (JsonElement ver : versions) {
                        JsonObject versionObj = ver.getAsJsonObject();

                        if (versionObj.get("id").getAsString().equals(mcVersion))
                            foundVersion = versionObj;
                    }

                    if (foundVersion == null)
                        throw new RuntimeException("Unable to find version: " + mcVersion);
                    wispApi.getVersionManifestUrl().set(foundVersion.get("url").getAsString());
                }

                var versionManifest = wisp.getMcCache("version.json").toFile();

                if (!versionManifest.exists()) {

                    ReadableByteChannel clientJarChannel = Channels.newChannel(new URL(wispApi.getVersionManifestUrl().get()).openStream());
                    versionManifest.getParentFile().mkdirs();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(versionManifest)) {

                        fileOutputStream.getChannel()
                                .transferFrom(clientJarChannel, 0, Long.MAX_VALUE);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                FileReader reader = new FileReader(versionManifest);
                wispApi.getVersion().set(MinecraftVersionManifest.fromJson(GSON.fromJson(reader, JsonObject.class)));

                reader.close();
//            downloadGame(new URL(getVersionManifest().get()), mcVersion, extension);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            logger.push();
            for (Map.Entry<TaskType, WispTask> task : wispApi.getSetupTasks().entrySet()) {
                logger.log("Running: " + task.getKey());
                logger.push();
                task.getValue().run();
                logger.pop();
            }

            var extractNatives = proj.getTasks().register("extractNatives", ExtractNativesTask.class, t -> {
                t.setDescription("Extracts the Minecraft platform specific natives.");
            });

            proj.getTasks().register("genSources", GenerateSourcesTask.class);

            var downloadResources = proj.getTasks().register("downloadResources", DownloadAssetsTask.class, downloadAssetsTask -> {

            });

            wispApi.getRunConfigs().forEach(runConfig -> {
                proj.getTasks().register("run" + runConfig.getName(), JavaExec.class, javaExec -> {
                    javaExec.setGroup("wisp");

                    javaExec.getMainClass().set(runConfig.getMainClass());
                    javaExec.args("--assetIndex " + wispApi.getVersion().get().assets() + " --assetsDir " + wisp.getMcCache("assets"));
                    javaExec.jvmArgs("-Djava.library.path=" + wisp.getMcCache("natives"));
                    javaExec.setWorkingDir(new File(proj.getProjectDir(), runConfig.getWorkingDir()));
                });
            });

            var mainProj = proj;

            boolean foundParent = false;
            while (!foundParent) {
                var parent = mainProj.getParent();
                if (parent == null) {
                    parent = mainProj;
                    foundParent = true;
                }
                mainProj = parent;
            }

            IdeaModel ideaModel = ((IdeaModel) mainProj.getExtensions().findByName("idea"));

            if (ideaModel == null) return;

            if (ideaModel.getProject() != null) {
                ProjectSettings settings = ((ExtensionAware) ideaModel.getProject()).getExtensions().getByType(ProjectSettings.class);
                NamedDomainObjectContainer<RunConfiguration> runConfigurations = (NamedDomainObjectContainer<RunConfiguration>)
                        ((ExtensionAware) settings).getExtensions().getByName("runConfigurations");

                Project finalMainProj = mainProj;
                wispApi.getRunConfigs().forEach(runConfig -> {
                    Application app = new Application(proj.getName() + ":" + runConfig.getDisplayName(), finalMainProj);
                    app.setMainClass(runConfig.getMainClass());
                    logger.log(getIdeaModuleName(runConfig.getSourceSet(), proj));
                    app.setModuleName(getIdeaModuleName(runConfig.getSourceSet(), proj));
                    app.setProgramParameters("--assetIndex " + wispApi.getVersion().get().assets() + " --assetsDir " + wisp.getMcCache("assets"));
                    app.setJvmArgs("-Djava.library.path=" + wisp.getMcCache("natives"));
                    app.setWorkingDirectory(runConfig.getWorkingDir());
                    runConfigurations.add(app);
                });
            }

            logger.pop();
            logger.log("Loaded Wisp Gradle");
        });


    }

    public static String getIdeaModuleName(SourceSet sourceSet, Project project) { // from loom I couldn't figure it out :P
        String module = project.getName() + "." + sourceSet.getName();

        while ((project = project.getParent()) != null) {
            module = project.getName() + "." + module;
        }

        return module.replace(' ', '_');
    }

    protected void downloadGame(URL address, String gameVersion, WispGradleApiExtension wisp) throws IOException {
        var logger = project.getLogger();
        JsonObject version = GSON.fromJson(WispExtensionApiImpl.readString(address), JsonObject.class);


        boolean includeGameJar = wisp.includeGameJar().get();


        List<String> classPaths = new ArrayList<>();

        String mcArgs;
        String javaArgs;

        if (version.has("minecraftArguments")) {
            mcArgs = version.get("minecraftArguments").getAsString();
            javaArgs = "";
        } else if (version.has("arguments")) {
            JsonObject arguments = version.getAsJsonObject("arguments");
            JvmArgs gameArgs = GSON.fromJson(arguments.getAsJsonArray("game"), JvmArgs.class);
//            JsonArray gameArgs = arguments.getAsJsonArray("game");
            JvmArgs jvmArgs = GSON.fromJson(arguments.getAsJsonArray("jvm"), JvmArgs.class);

            System.out.println(jvmArgs);

            StringBuilder args = new StringBuilder();
            for (Iterator<JvmArgs.Argument> it = gameArgs.arguments().iterator(); it.hasNext(); ) {
                JvmArgs.Argument arg = it.next();
                args.append(arg.toString());
                if (it.hasNext())
                    args.append(" ");
            }

            StringBuilder jvm = new StringBuilder();
            for (Iterator<JvmArgs.Argument> it = jvmArgs.arguments().iterator(); it.hasNext(); ) {
                JvmArgs.Argument arg = it.next();
                jvm.append(arg.toString());
                if (it.hasNext())
                    jvm.append(" ");
            }

            mcArgs = args.toString().replace("${assets_root}", project.getRootDir().toPath().resolve("wisp/assets").toString()).replace("${assets_index_name}", version.get("assets").getAsString()).replace("${version_type}", version.get("type").getAsString()).replace("${game_directory}", "./");

            javaArgs = jvm.toString(); /*+ " " + logging.get("argument").getAsString().replace("${path}", logArg)*/
        } else {
            mcArgs = "";
            javaArgs = "";
        }


        JsonObject assetIndex = version.getAsJsonObject("assetIndex");

//        File assetFile = assetDir.resolve("indexes/" + assetIndex.get("id").getAsString() + ".json").toFile();
//        if (!assetFile.exists()) {
//            System.out.println("Downloading asset file");
//            logger.info("Downloading asset file");
//            try {
//                ReadableByteChannel assetIndexChannel = Channels.newChannel(new URL(assetIndex.get("url").getAsString()).openStream());
//
//                assetFile.getParentFile().mkdirs();
//                try (FileOutputStream fileOutputStream = new FileOutputStream(assetFile)) {
//
//                    fileOutputStream.getChannel()
//                            .transferFrom(assetIndexChannel, 0, Long.MAX_VALUE);
//                }
//            } catch (IOException e) {
//                throw new RuntimeException("Failed to download asset index");
//            }
//        }


//        try {
//
//        } catch (FileNotFoundException e) {
//
//        }
        System.out.println("Finished downloading assets");

        if (version.has("logging")) {
            JsonObject logging = version.getAsJsonObject("logging").getAsJsonObject("client");
            var loggingConfig = project.getRootDir().toPath().resolve("wisp/logging/" + logging.getAsJsonObject("file").get("id").getAsString()).toFile();
            String logArg = loggingConfig.toString();

            if (!loggingConfig.exists()) {
                try {
                    ReadableByteChannel clientJarChannel = Channels.newChannel(new URL(logging.getAsJsonObject("file").getAsJsonObject().get("url").getAsString()).openStream());
                    loggingConfig.getParentFile().mkdirs();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(loggingConfig)) {

                        fileOutputStream.getChannel()
                                .transferFrom(clientJarChannel, 0, Long.MAX_VALUE);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to download log config");
                }
            }
        }

        project.afterEvaluate(proj -> {

        });
    }
}