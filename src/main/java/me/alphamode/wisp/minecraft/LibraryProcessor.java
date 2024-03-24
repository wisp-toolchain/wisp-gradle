package me.alphamode.wisp.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class LibraryProcessor {
    private static final List<Predicate<Library>> PROCESSORS = new ArrayList<>();

    public static boolean process(Library library) {
        for (Predicate<Library> processor : PROCESSORS)
            if (!processor.test(library))
                return false;
        return true;
    }

    public static void registerProcessor(Predicate<Library> processor) {
        PROCESSORS.add(processor);
    }

    static {
        registerProcessor(new LegacyASMLibraryProcessor());
    }
}
