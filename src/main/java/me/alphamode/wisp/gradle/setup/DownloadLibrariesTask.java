package me.alphamode.wisp.gradle.setup;

import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.minecraft.Library;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

public class DownloadLibrariesTask extends WispTask {
    public static final String DOWNLOAD_LIBRARIES_TASK = "downloadLibraries";

    public DownloadLibrariesTask(Project project) {
        super(project);
    }

    @TaskAction
    public void run() {
        var wisp = WispGradleApiExtension.get(this.project);
        wisp.getLogger().log("Downloading libraries");
        List<Library> libs = wisp.getVersion().get().libraries();
        for (Library lib : libs) {
            var downloadPath = WispGradleExtension.get(project).getMcCache("libs/");
            var download = lib.download();
            var path = download.path() != null ? downloadPath.resolve(download.path()).toFile() : downloadPath.toFile();
            if (!path.exists()) {
                wisp.getLogger().log("Downloading: " + lib);
                downloader.downloadArtifact(download, path);
            }
        }
    }
}
