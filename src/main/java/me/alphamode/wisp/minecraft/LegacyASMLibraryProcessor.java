package me.alphamode.wisp.minecraft;

import java.util.function.Predicate;

/**
 * Never depend on the legacy "asm-all".
 */
public class LegacyASMLibraryProcessor implements Predicate<Library> { // Yes I stole this from loom
    private static final String LEGACY_ASM = "org.ow2.asm:asm-all";

    @Override
    public boolean test(Library library) {
        return !library.name().startsWith(LEGACY_ASM);
    }
}