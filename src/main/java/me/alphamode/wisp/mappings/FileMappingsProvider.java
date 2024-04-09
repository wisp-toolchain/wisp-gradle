package me.alphamode.wisp.mappings;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMappingsProvider implements MappingProvider {
    private final MemoryMappingTree clientMappings, serverMappings;

    public FileMappingsProvider(Path clientMappings, Path serverMappings) {
        this.clientMappings = new MemoryMappingTree();
        this.serverMappings = new MemoryMappingTree();
        try (Reader reader = Files.newBufferedReader(clientMappings, StandardCharsets.UTF_8)) {
            MappingReader.read(reader, this.clientMappings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read client mappings", e);
        }
        try (Reader reader = Files.newBufferedReader(serverMappings, StandardCharsets.UTF_8)) {
            MappingReader.read(reader, this.serverMappings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read client mappings", e);
        }
    }

    @Override
    public MemoryMappingTree getClientMappings() {
        return this.clientMappings;
    }

    @Override
    public MemoryMappingTree getServerMappings() {
        return this.serverMappings;
    }
}
