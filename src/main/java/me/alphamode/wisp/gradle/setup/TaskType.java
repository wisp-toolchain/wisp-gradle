package me.alphamode.wisp.gradle.setup;

public enum TaskType {
    DOWNLOAD_LIBRARIES,
    DOWNLOAD_GAME,
    REMAP_GAME(true),
    MERGE_JARS(true);

    final boolean optional;

    TaskType(boolean optional) {
        this.optional = optional;
    }

    TaskType() {
        this(false);
    }

    public boolean isOptional() {
        return optional;
    }
}
