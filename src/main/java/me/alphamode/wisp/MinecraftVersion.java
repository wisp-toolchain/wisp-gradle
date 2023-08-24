package me.alphamode.wisp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MinecraftVersion {
    public Arguments arguments;
    public String assets;
    public int complianceLevel;

    public MinecraftJars downloads;

    public AssetIndex assetIndex;
    public String id;

    public List<Library> libraries;

    public static class Arguments {
        public List<Argument> game;
        public List<Argument> jvm;
    }

    public static class Argument {

    }

    public static class MinecraftJars {
        public Download client;

        @SerializedName("client_mappings")
        public Download clientMappings;

        public Download server;

        @SerializedName("server_mappings")
        public Download serverMappings;
    }

    public static class Library {
        public String name;
        public Downloads downloads;
    }

    public static class Downloads {
        public Download artifact;
    }

    public static class AssetIndex {
        public String id;
        public String sha1;
        public long size;
        public String url;
    }
}
