package me.alphamode.wisp.jar;

import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.gradle.setup.*;
import me.alphamode.wisp.jar.minecraft.MinecraftJarProvider;
import org.gradle.api.Project;

import java.util.List;

public class MinecraftProvider implements MinecraftJarProvider {
    @Override
    public void provide(Project proj) {
        WispGradleApiExtension wisp = WispGradleApiExtension.get(proj);
        var logger = wisp.getLogger();

        List<WispTask> setup = List.of(
                new DownloadLibrariesTask(proj),
                new DownloadGameTask(proj),
                new RemapGameTask(proj),
                new MergeJarsTask(proj)
        );

        logger.push();
        for (WispTask task : setup) {
            logger.log("Running: " + task.getClass().getSimpleName());
            logger.push();
            task.run();
            logger.pop();
        }
        logger.pop();
    }
}
