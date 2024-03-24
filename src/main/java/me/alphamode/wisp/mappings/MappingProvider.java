package me.alphamode.wisp.mappings;

import java.nio.file.Path;

public interface MappingProvider {
    Path getClientMappings();

    Path getServerMappings();
}
