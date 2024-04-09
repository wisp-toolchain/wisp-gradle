package me.alphamode.wisp;

import java.nio.file.Path;

public class WispExtensionImpl implements WispGradleExtension {
    private final WispGradleApiExtension extension;

    public WispExtensionImpl(WispGradleApiExtension extension) {
        this.extension = extension;;
    }

    @Override
    public Path getCache(String path) {
        return extension.getCacheDirectory().get().getAsFile().toPath().resolve(path);
    }

    @Override
    public String getMcVersion() {
        return extension.getMinecraftVersion().get();
    }

    @Override
    public boolean hasIntegratedServer() {
        return true; // TODO
    }
}
