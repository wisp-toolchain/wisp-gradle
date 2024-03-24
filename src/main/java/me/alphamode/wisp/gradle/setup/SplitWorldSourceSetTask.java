package me.alphamode.wisp.gradle.setup;

import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.jar.JarMerger;
import me.alphamode.wisp.util.WispConstants;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class SplitWorldSourceSetTask extends WispTask {
    private final File client, server, merged;

    @Inject
    public SplitWorldSourceSetTask(Project project) {
        super(project);
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        sourceSets.create(WispConstants.CLIENT_SOURCE_SET, sourceSet -> {
            sourceSets.add(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
            sourceSet.getJava().srcDir(project.file("src/client/java"));
            sourceSet.getResources().srcDir(project.file("src/client/resources"));
        });

        WispGradleExtension wisp = WispGradleExtension.get(this.project);
        this.client = wisp.getMcCache("client/client-mapped-" + wisp.getMcVersion() + ".jar").toFile();
        this.server = wisp.getMcCache("server/server-mapped-" + wisp.getMcVersion() + ".jar").toFile();
        this.merged = wisp.getMcCache("merged/merged.jar").toFile();
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

            this.project.getDependencies().add("implementation", this.project.files(this.merged));
        }
    }
}
