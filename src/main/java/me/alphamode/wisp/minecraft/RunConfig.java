package me.alphamode.wisp.minecraft;

// TODO: programArgs and jvm args
public class RunConfig {
    private final String name;

    private String mainClass = "me.alphamode.wisp.loader.Main";

    private String displayName;

    private String workingDir = "run/";

    private Type type;

    public RunConfig(String name) {
        this.name = name;
    }

    public void client() {
        displayName = "Minecraft Client";
        type = Type.CLIENT;
    }

    public void server() {
        displayName = "Minecraft Server";
        type = Type.SERVER;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public Type getType() {
        return type;
    }

    public void mainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void displayName(String displayName) {
        this.displayName = displayName;
    }

    public void workingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public void type(Type type) {
        this.type = type;
    }

    public enum Type {
        CLIENT,
        SERVER
    }
}
