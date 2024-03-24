package me.alphamode.wisp.gradle.setup;

import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.api.MinecraftJars;
import me.alphamode.wisp.jar.JarMerger;
import me.alphamode.wisp.util.WispConstants;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MergeJarsTask extends WispTask {
    private final File client, server, merged;

    @Inject
    public MergeJarsTask(Project project) {
        super(project);
        WispGradleExtension wisp = WispGradleExtension.get(this.project);
        this.client = wisp.getMcCache("client/client-mapped-" + wisp.getMcVersion() + ".jar").toFile();
        this.server = wisp.getMcCache("server/server-mapped-" + wisp.getMcVersion() + ".jar").toFile();
        this.merged = wisp.getMcCache("merged/merged" + wisp.getMcVersion() + ".jar").toFile();
    }

    @Override
    public void run() {
        WispGradleApiExtension wisp = WispGradleApiExtension.get(this.project);
        if (!wisp.clientOnly().get() && !wisp.serverOnly().get()) {
            if (!this.merged.exists()) {

                this.merged.getParentFile().mkdirs();

                try (JarMerger jarMerger = new JarMerger(this.client.toPath(), this.server.toPath(), this.merged)) {
                    jarMerger.enableSyntheticParamsOffset();
                    jarMerger.merge();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            wisp.getModProcessor().process(this.merged.toPath());
            this.project.getDependencies().add(WispConstants.MINECRAFT, this.project.files(this.merged));
            wisp.setMinecraft(new MinecraftJars(List.of(this.merged.toPath())));
        }
    }
}
