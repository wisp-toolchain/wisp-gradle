package me.alphamode.wisp.gradle.setup;

import me.alphamode.wisp.Downloader;
import org.gradle.api.Project;

public abstract class WispTask {
    protected final Project project;
    protected final Downloader downloader;

    public WispTask(Project project) {
        this.project = project;
         downloader = new Downloader(project);
    }

    public abstract void run();
}
