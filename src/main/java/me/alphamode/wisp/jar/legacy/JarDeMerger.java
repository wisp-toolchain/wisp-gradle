package me.alphamode.wisp.jar.legacy;

import me.alphamode.wisp.FileSystemUtil;
import me.alphamode.wisp.jar.JarMerger;
import me.alphamode.wisp.jar.MinecraftClassMerger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JarDeMerger implements AutoCloseable {
    private static final MinecraftLegacyClassMerger CLASS_MERGER = new MinecraftLegacyClassMerger();
    private final FileSystemUtil.Delegate inputClientFs, inputServerFs, outputFs;
    private final Path inputClient, inputServer;
    private final Map<String, JarMerger.Entry> entriesClient, entriesServer;
    private final Set<String> entriesAll;
    public JarDeMerger(Path inputClient, Path inputServer, File output) throws IOException {
        if (output.exists()) {
            if (!output.delete()) {
                throw new IOException("Could not delete " + output.getName());
            }
        }

        this.inputClient = (inputClientFs = FileSystemUtil.getJarFileSystem(inputClient, false)).get().getPath("/");
        this.inputServer = (inputServerFs = FileSystemUtil.getJarFileSystem(inputServer, false)).get().getPath("/");
        this.outputFs = FileSystemUtil.getJarFileSystem(output, true);

        this.entriesClient = new HashMap<>();
        this.entriesServer = new HashMap<>();
        this.entriesAll = new TreeSet<>();
    }

    @Override
    public void close() throws IOException {
        inputClientFs.close();
        inputServerFs.close();
        outputFs.close();
    }

    private void readToMap(Map<String, JarMerger.Entry> map, Path input) {
        try {
            Files.walkFileTree(input, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
                    if (attr.isDirectory()) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (!path.getFileName().toString().endsWith(".class")) {
                        if (path.toString().equals("/META-INF/MANIFEST.MF")) {
                            map.put("META-INF/MANIFEST.MF", new JarMerger.Entry(path, attr,
                                    "Manifest-Version: 1.0\nMain-Class: net.minecraft.client.Main\n".getBytes(StandardCharsets.UTF_8)));
                        } else {
                            if (path.toString().startsWith("/META-INF/")) {
                                if (path.toString().endsWith(".SF") || path.toString().endsWith(".RSA")) {
                                    return FileVisitResult.CONTINUE;
                                }
                            }

                            map.put(path.toString().substring(1), new JarMerger.Entry(path, attr, null));
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    byte[] output = Files.readAllBytes(path);
                    map.put(path.toString().substring(1), new JarMerger.Entry(path, attr, output));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void add(JarMerger.Entry entry) throws IOException {
        Path outPath = outputFs.get().getPath(entry.path.toString());

        if (outPath.getParent() != null) {
            Files.createDirectories(outPath.getParent());
        }

        if (entry.data != null) {
            Files.write(outPath, entry.data, StandardOpenOption.CREATE_NEW);
        } else {
            Files.copy(entry.path, outPath);
        }

        Files.getFileAttributeView(outPath, BasicFileAttributeView.class)
                .setTimes(
                        entry.metadata.creationTime(),
                        entry.metadata.lastAccessTime(),
                        entry.metadata.lastModifiedTime()
                );
    }

    public void merge() throws IOException {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(() -> readToMap(entriesClient, inputClient));
        service.submit(() -> readToMap(entriesServer, inputServer));
        service.shutdown();

        try {
            service.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        entriesAll.addAll(entriesClient.keySet());
        entriesAll.addAll(entriesServer.keySet());

        List<JarMerger.Entry> entries = entriesAll.parallelStream().map((entry) -> {
            boolean isClass = entry.endsWith(".class");
            boolean isMinecraftWorld = entriesClient.containsKey(entry) || entry.startsWith("net/minecraft/world") || !entry.contains("/");
            JarMerger.Entry result;
            String side = null;

            JarMerger.Entry entry1 = entriesClient.get(entry);
            JarMerger.Entry entry2 = entriesServer.get(entry);

            if (entry1 != null && entry2 != null) {
                if (Arrays.equals(entry1.data, entry2.data)) {
                    result = entry1;
                } else {
                    if (isClass) {
                        result = new JarMerger.Entry(entry1.path, entry1.metadata, CLASS_MERGER.mergeWorld(entry1.data, entry2.data));
                    } else {
                        // FIXME: More heuristics?
                        result = entry1;
                    }
                }
            } else if ((result = entry1) != null) {
                side = "CLIENT";
            } else if ((result = entry2) != null) {
                side = "SERVER";
            }

            if (isClass && !isMinecraftWorld && "SERVER".equals(side)) {
                // Server bundles libraries, client doesn't - skip them
                return null;
            }

            if (result != null) {
                if (isMinecraftWorld && isClass) {
                    byte[] data = result.data;
                    ClassReader reader = new ClassReader(data);
                    ClassWriter writer = new ClassWriter(0);
                    ClassVisitor visitor = writer;

                    if (side != null) {
                        visitor = new MinecraftClassMerger.SidedClassVisitor(Opcodes.ASM9, visitor, side);
                    }

//                    if (removeSnowmen) {
//                        visitor = new SnowmanClassVisitor(Opcodes.ASM9, visitor);
//                    }
//
//                    if (offsetSyntheticsParams) {
//                        visitor = new SyntheticParameterClassVisitor(Opcodes.ASM9, visitor);
//                    }

                    if (visitor != writer) {
                        reader.accept(visitor, 0);
                        data = writer.toByteArray();
                        result = new JarMerger.Entry(result.path, result.metadata, data);
                    }
                }

                return result;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).toList();

        for (JarMerger.Entry e : entries) {
            add(e);
        }
    }
}
