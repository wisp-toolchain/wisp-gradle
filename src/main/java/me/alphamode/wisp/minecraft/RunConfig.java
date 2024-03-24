package me.alphamode.wisp.minecraft;

public class RunConfig {
    private final String name;
    private String displayName;

    public RunConfig(String name) {
        this.name = name;
    }

    public void client() {
        displayName = "Minecraft Client";
    }

    public void server() {
        displayName = "Minecraft Server";
    }
}
