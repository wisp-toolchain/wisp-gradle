package me.alphamode.wisp.mappings;

import java.nio.file.Path;

public class FileMappingsProvider implements MappingProvider {
    private final Path clientMappings, serverMappings;

    public FileMappingsProvider(Path clientMappings, Path serverMappings) {
        this.clientMappings = clientMappings;
        this.serverMappings = serverMappings;
    }

    @Override
    public Path getClientMappings() {
        return this.clientMappings;
    }

    @Override
    public Path getServerMappings() {
        return this.serverMappings;
    }
}
