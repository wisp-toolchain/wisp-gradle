package me.alphamode.wisp.gradle;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class WispLogger {
    private final Logger logger;
    public WispLogger(Project project) {
        this.logger = project.getLogger();
    }

    private int currentIndention;

    public void push() {
        this.currentIndention++;
    }

    public void pop() {
        this.currentIndention--;
    }

    public void log(Object msg) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < currentIndention; i++) {
            stringBuilder.append("  ");
        }
        this.logger.lifecycle(stringBuilder.append(currentIndention == 0 ? "> " : "- ").append(msg).toString());
    }

    public void error(String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < currentIndention; i++) {
            stringBuilder.append("  ");
        }
        this.logger.error(stringBuilder.append("- ").append(msg).toString());
    }
}
