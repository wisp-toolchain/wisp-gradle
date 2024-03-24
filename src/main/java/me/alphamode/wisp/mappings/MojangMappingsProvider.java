package me.alphamode.wisp.mappings;

import me.alphamode.wisp.AddConstructorMappingVisitor;
import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.WispGradleExtension;
import me.alphamode.wisp.minecraft.Download;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import org.gradle.api.Project;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MojangMappingsProvider implements MappingProvider {
    private final Project project;

    public MojangMappingsProvider(Project project) {
        this.project = project;
    }

    @Override
    public Path getServerMappings() {
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
            return serverMappings;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getClientMappings() {
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
            return clientMappings;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
