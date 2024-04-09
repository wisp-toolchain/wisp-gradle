package me.alphamode.wisp.mappings;

import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

public interface MappingProvider {
    MemoryMappingTree getClientMappings();

    MemoryMappingTree getServerMappings();
}
