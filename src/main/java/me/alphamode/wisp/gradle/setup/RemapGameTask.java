package me.alphamode.wisp.gradle.setup;

import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.api.MinecraftJars;
import me.alphamode.wisp.mappings.MappingProvider;
import me.alphamode.wisp.util.WispConstants;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RemapGameTask extends WispTask {
    public static final String REMAP_GAME_TASK = "remapGame";

    private final File client, server, outputClient, outputServer;

    public File getMappedClient() {
        return this.outputClient;
    }


    public File getMappedServer() {
        return this.outputServer;
    }

    @Inject
    public RemapGameTask(Project project) {
        super(project);
        WispGradleExtension wisp = WispGradleExtension.get(project);
        this.client = wisp.getMcCache("client/client-" + wisp.getMcVersion() + ".jar").toFile();
        this.server = wisp.getMcCache("server/server-" + wisp.getMcVersion() + ".jar").toFile();
        this.outputClient = wisp.getMcCache("client/client-mapped-" + wisp.getMcVersion() + ".jar").toFile();
        this.outputServer = wisp.getMcCache("server/server-mapped-" + wisp.getMcVersion() + ".jar").toFile();
    }

    @Override
    public void run() {
        WispGradleApiExtension wisp = WispGradleApiExtension.get(this.project);
        MappingProvider mappingProvider = wisp.getMappingsProvider().get();
        Path mappedClient = getMappedClient().toPath();
        Path mappedServer = getMappedServer().toPath();
        if (!wisp.serverOnly().get() && wisp.includeGameJar().get()) {
            if (!Files.exists(mappedClient)) {
                wisp.getLogger().log("Remapping client");
                var clientMappings = mappingProvider.getClientMappings();

                var clientRemapper = TinyRemapper.newRemapper()
                        .withMappings(TinyUtils.createTinyMappingProvider(clientMappings, "official", "named"))
                        .renameInvalidLocals(true)
                        .rebuildSourceFilenames(true)
                        .build();

                try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(mappedClient)
                        // force jar despite the .tmp extension
                        .assumeArchive(true)
                        .build()) {

                    for (File gameLib : this.project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)) {
                        clientRemapper.readClassPath(gameLib.toPath());
                    }

                    outputConsumer.addNonClassFiles(client.toPath(), NonClassCopyMode.FIX_META_INF, clientRemapper);
                    clientRemapper.readInputs(client.toPath());

                    clientRemapper.apply(outputConsumer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    clientRemapper.finish();
                }
            }
            if (wisp.clientOnly().get()) {
                wisp.getModProcessor().process(mappedClient);
                this.project.getDependencies().add(WispConstants.MINECRAFT, this.project.files(mappedClient));
                wisp.setMinecraft(new MinecraftJars(List.of(mappedClient)));
            }
        }

        if (!wisp.clientOnly().get() && wisp.includeGameJar().get()) {


            var serverMappings = mappingProvider.getServerMappings();

            var serverRemapper = TinyRemapper.newRemapper()
                    .withMappings(TinyUtils.createTinyMappingProvider(serverMappings, "official", "named"))
                    .rebuildSourceFilenames(true)
                    .build();

            if (!mappedServer.toFile().exists()) {
                wisp.getLogger().log("Remapping server");

                try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(mappedServer)
                        // force jar despite the .tmp extension
                        .assumeArchive(true)
                        .build()) {

                    for (File gameLib : this.project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)) {
                        serverRemapper.readClassPath(gameLib.toPath());
                    }

                    outputConsumer.addNonClassFiles(server.toPath(), NonClassCopyMode.FIX_META_INF, serverRemapper);
                    serverRemapper.readInputs(server.toPath());

                    serverRemapper.apply(outputConsumer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    serverRemapper.finish();
                }

            }
            if (wisp.serverOnly().get()) {
                wisp.getModProcessor().process(mappedServer);
                this.project.getDependencies().add(WispConstants.MINECRAFT, this.project.files(mappedServer));
                wisp.setMinecraft(new MinecraftJars(List.of(mappedServer)));
            }
        }
    }
}
