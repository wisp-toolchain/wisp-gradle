package me.alphamode.wisp.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarMerger {
    public static void mergeJar(Path client, Path server, Path output) {
        try {
            JarFile clientJar = new JarFile(client.toFile());
            JarFile serverJar = new JarFile(server.toFile());

            List<JarEntry> common = new ArrayList<>();

            clientJar.stream().forEach(jarEntry -> {
                JarEntry entry = serverJar.getJarEntry(jarEntry.getName());
                if (entry != null)
                    common.add(entry);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
