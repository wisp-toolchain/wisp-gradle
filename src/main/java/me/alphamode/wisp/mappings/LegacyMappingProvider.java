package me.alphamode.wisp.mappings;

import java.nio.file.Path;

/**
 * Mapping Provider for versions pre 1.3
 */
public interface LegacyMappingProvider {
    Path getIntermediaryMappings();

    Path getMappings();
}