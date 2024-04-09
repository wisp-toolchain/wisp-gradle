package me.alphamode.wisp.mappings;

import me.alphamode.wisp.AddConstructorMappingVisitor;
import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.minecraft.Download;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.gradle.api.Project;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MojangMappingsProvider implements MappingProvider {
    private final Project project;
    private MemoryMappingTree cachedClientMappings, cachedServerMappings;

    public MojangMappingsProvider(Project project) {
        this.project = project;
    }

    @Override
    public MemoryMappingTree getServerMappings() {
        if (cachedServerMappings == null) {
            Download download = WispGradleApiExtension.get(project).getVersion().get().getDownload("server_mappings");
            WispGradleExtension wisp = WispGradleExtension.get(project);
            Path serverMappingsProguard = wisp.getMcCache("server/mapping.txt");
            Path serverMappings = wisp.getMcCache("server/mapping.tiny");
            try {
                Files.write(serverMappingsProguard, new URL(download.url()).openStream().readAllBytes());
                try (Writer writer = new StringWriter()) {
                    MappingWriter mappingTree = new Tiny2FileWriter(writer, false);
                    MappingSourceNsSwitch sourceNsSwitch = new MappingSourceNsSwitch(mappingTree, "named", true);
                    AddConstructorMappingVisitor constructorMappingVisitor = new AddConstructorMappingVisitor(sourceNsSwitch);
                    ProGuardFileReader.read(new FileReader(serverMappingsProguard.toFile()), "named", "official", constructorMappingVisitor);
                    Files.write(serverMappings, writer.toString().getBytes(StandardCharsets.UTF_8));
                }
                this.cachedServerMappings = new MemoryMappingTree();
                Reader reader = Files.newBufferedReader(serverMappings, StandardCharsets.UTF_8);
                MappingReader.read(reader, this.cachedServerMappings);
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read server mappings", e);
            }
        }
        return this.cachedServerMappings;
    }

    @Override
    public MemoryMappingTree getClientMappings() {
        Download download = WispGradleApiExtension.get(project).getVersion().get().getDownload("client_mappings");
        WispGradleExtension wisp = WispGradleExtension.get(project);
        Path clientMappingsProguard = wisp.getMcCache("client/mapping.txt");
        Path clientMappings = wisp.getMcCache("client/mapping.tiny");
        try {
            Files.write(clientMappingsProguard, new URL(download.url()).openStream().readAllBytes());

            try (Writer writer = new StringWriter()) {
                MappingWriter mappingTree = new Tiny2FileWriter(writer, false);
                MappingSourceNsSwitch sourceNsSwitch = new MappingSourceNsSwitch(mappingTree, "named", true);
                AddConstructorMappingVisitor constructorMappingVisitor = new AddConstructorMappingVisitor(sourceNsSwitch);
                ProGuardFileReader.read(new FileReader(clientMappingsProguard.toFile()), "named", "official", constructorMappingVisitor);
                Files.write(clientMappings, writer.toString().getBytes(StandardCharsets.UTF_8));
            }
            this.cachedClientMappings = new MemoryMappingTree();
            Reader reader = Files.newBufferedReader(clientMappings, StandardCharsets.UTF_8);
            MappingReader.read(reader, this.cachedClientMappings);
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read client mappings", e);
        }

        return this.cachedClientMappings;
    }
}
