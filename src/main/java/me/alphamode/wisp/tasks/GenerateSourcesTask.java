package me.alphamode.wisp.tasks;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import me.alphamode.wisp.FileSystemUtil;
import me.alphamode.wisp.WispGradleApiExtension;
import me.alphamode.wisp.api.MinecraftJars;
import me.alphamode.wisp.decompiler.LineNumberRemapper;
import me.alphamode.wisp.decompiler.vineflower.VineflowerDecompiler;
import me.alphamode.wisp.util.WispConstants;
import net.fabricmc.mappingio.format.tiny.Tiny1FileWriter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.gradle.workers.internal.WorkerDaemonClientsManager;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

@DisableCachingByDefault
public abstract class GenerateSourcesTask extends DefaultTask {
    /**
     * The jar name to decompile, {@link MinecraftJar#getName()}.
     */
    @Input
    public abstract Property<String> getInputJarName();

    @InputFiles
    public abstract ConfigurableFileCollection getClasspath();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    // Unpick
    @InputFile
    @Optional
    public abstract RegularFileProperty getUnpickDefinitions();

    @InputFiles
    @Optional
    public abstract ConfigurableFileCollection getUnpickConstantJar();

    @InputFiles
    @Optional
    public abstract ConfigurableFileCollection getUnpickClasspath();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getUnpickOutputJar();

    // Injects
    @Inject
    public abstract WorkerExecutor getWorkerExecutor();

    @Inject
    public abstract ExecOperations getExecOperations();

    @Inject
    public abstract WorkerDaemonClientsManager getWorkerDaemonClientsManager();

    @Inject
    public GenerateSourcesTask() {

        getOutputs().upToDateWhen((o) -> false);
        getInputJarName().set("merged.jar");
        getOutputJar().set(getProject().file("wisp/merged/sources.jar"));
//        getClasspath().from(decompilerOptions.getClasspath()).finalizeValueOnRead();
//        dependsOn(decompilerOptions.getClasspath().getBuiltBy());
    }

    @TaskAction
    public void run() throws IOException {

        final MinecraftJars minecraftJar = rebuildInputJar();
        // Input jar is the jar to decompile, this may be unpicked.
        Path inputJar = minecraftJar.jars().get(0).jar();//.getPath();
        // Runtime jar is the jar used to run the game
        final Path runtimeJar = inputJar;

        if (getUnpickDefinitions().isPresent()) {
            inputJar = unpickJar(inputJar);
        }

        // Set up the IPC path to get the log output back from the forked JVM
        final Path ipcPath = Files.createTempFile("loom", "ipc");
        Files.deleteIfExists(ipcPath);

        doWork(inputJar, runtimeJar);
    }

    // Re-run the named minecraft provider to give us a fresh jar to decompile.
    // This prevents re-applying line maps on an existing jar.
    private MinecraftJars rebuildInputJar() {
//        final MinecraftJars minecraftJars = WispGradleExtension.get(getProject()).getMinecraft();
//
//        for (MinecraftJar minecraftJar : minecraftJars) {
//            if (minecraftJar.getName().equals(getInputJarName().get())) {
//                return minecraftJar;
//            }
//        }
//
//        throw new IllegalStateException("Could not find minecraft jar (%s) but got (%s)".formatted(
//                getInputJarName().get(),
//                minecraftJars.stream().map(MinecraftJar::getName).collect(Collectors.joining(", ")))
//        );
        return WispGradleApiExtension.get(getProject()).getMinecraft();
    }

    private Path unpickJar(Path inputJar) {
        final Path outputJar = getUnpickOutputJar().get().getAsFile().toPath();
        final List<String> args = getUnpickArgs(inputJar, outputJar);

        ExecResult result = getExecOperations().javaexec(spec -> {
            spec.getMainClass().set("daomephsta.unpick.cli.Main");
            spec.classpath(getProject().getConfigurations().getByName(WispConstants.UNPICK_CLASSPATH));
            spec.args(args);
//            spec.systemProperty("java.util.logging.config.file", writeUnpickLogConfig().getAbsolutePath());
        });

        result.rethrowFailure();

        return outputJar;
    }

    private List<String> getUnpickArgs(Path inputJar, Path outputJar) {
        var fileArgs = new ArrayList<File>();

        fileArgs.add(inputJar.toFile());
        fileArgs.add(outputJar.toFile());
        fileArgs.add(getUnpickDefinitions().get().getAsFile());
        fileArgs.add(getUnpickConstantJar().getSingleFile());

        // Classpath
        for (MinecraftJars.MinecraftJar minecraftJar : WispGradleApiExtension.get(getProject()).getMinecraft().jars()) {
            fileArgs.add(minecraftJar.jar().toFile());
        }

        for (File file : getUnpickClasspath()) {
            fileArgs.add(file);
        }

        return fileArgs.stream()
                .map(File::getAbsolutePath)
                .toList();
    }

//    private File writeUnpickLogConfig() {
//        final File unpickLoggingConfigFile = getExtension().getFiles().getUnpickLoggingConfigFile();
//
//        try (InputStream is = GenerateSourcesTask.class.getClassLoader().getResourceAsStream("unpick-logging.properties")) {
//            Files.deleteIfExists(unpickLoggingConfigFile.toPath());
//            Files.copy(Objects.requireNonNull(is), unpickLoggingConfigFile.toPath());
//        } catch (IOException e) {
//            throw new org.gradle.api.UncheckedIOException("Failed to copy unpick logging config", e);
//        }
//
//        return unpickLoggingConfigFile;
//    }

    private void doWork(Path inputJar, Path runtimeJar) {
        final String jvmMarkerValue = UUID.randomUUID().toString();
        final WorkQueue workQueue = createWorkQueue(jvmMarkerValue);

        workQueue.submit(DecompileAction.class, params -> {
            params.getInputJar().set(inputJar.toFile());
            params.getRuntimeJar().set(runtimeJar.toFile());
            params.getSourcesDestinationJar().set(getOutputJar());
            params.getLinemap().set(getMappedJarFileWithSuffix("-sources.lmap", runtimeJar));
            params.getLinemapJar().set(getMappedJarFileWithSuffix("-linemapped.jar", runtimeJar));
            params.getMappings().set(getMappings().toFile());

            params.getClassPath().setFrom(getProject().getConfigurations().getByName(WispConstants.MINECRAFT_LIBRARIES));
        });

        workQueue.await();
        getProject().getDependencies().add(WispConstants.MINECRAFT_LIBRARIES, getOutputJar().get());
    }

    private WorkQueue createWorkQueue(String jvmMarkerValue) {
        if (!useProcessIsolation()) {
            return getWorkerExecutor().classLoaderIsolation(spec -> {
                spec.getClasspath().from(getClasspath());
            });
        }

        return getWorkerExecutor().processIsolation(spec -> {
            spec.forkOptions(forkOptions -> {
                forkOptions.setMinHeapSize(String.format(Locale.ENGLISH, "%dm", Math.min(512, 4096L)));
                forkOptions.setMaxHeapSize(String.format(Locale.ENGLISH, "%dm", 4096L));
//                forkOptions.systemProperty(WorkerDaemonClientsManagerHelper.MARKER_PROP, jvmMarkerValue);
            });
            spec.getClasspath().from(getClasspath());
        });
    }

    private boolean useProcessIsolation() {
        // Useful if you want to debug the decompiler, make sure you run gradle with enough memory.
        return !Boolean.getBoolean("fabric.loom.genSources.debug");
    }

    public interface DecompileParams extends WorkParameters {

        RegularFileProperty getInputJar();
        RegularFileProperty getRuntimeJar();
        RegularFileProperty getSourcesDestinationJar();
        RegularFileProperty getLinemap();
        RegularFileProperty getLinemapJar();
        RegularFileProperty getMappings();

        RegularFileProperty getIPCPath();

        ConfigurableFileCollection getClassPath();
    }

    public abstract static class DecompileAction implements WorkAction<DecompileParams> {
        @Override
        public void execute() {
            doDecompile();
        }

        private void doDecompile() {
            final Path inputJar = getParameters().getInputJar().get().getAsFile().toPath();
            final Path sourcesDestinationJar = getParameters().getSourcesDestinationJar().get().getAsFile().toPath();
            final Path linemap = getParameters().getLinemap().get().getAsFile().toPath();
            final Path linemapJar = getParameters().getLinemapJar().get().getAsFile().toPath();
            final Path runtimeJar = getParameters().getRuntimeJar().get().getAsFile().toPath();

            final VineflowerDecompiler decompiler = new VineflowerDecompiler();

            decompiler.decompile(
                    inputJar,
                    sourcesDestinationJar,
                    linemap,
                    getParameters().getMappings().get().getAsFile().toPath(),
                    getLibraries()
            );

            // Close the decompile loggers

            if (Files.exists(linemap)) {
                try {
                    // Line map the actually jar used to run the game, not the one used to decompile
                    remapLineNumbers(runtimeJar, linemap, linemapJar);

                    Files.copy(linemapJar, runtimeJar, StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(linemapJar);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to remap line numbers", e);
                }
            }
        }

        private void remapLineNumbers(Path oldCompiledJar, Path linemap, Path linemappedJarDestination) throws IOException {
            LineNumberRemapper remapper = new LineNumberRemapper();
            remapper.readMappings(linemap.toFile());

            try (FileSystemUtil.Delegate inFs = FileSystemUtil.getJarFileSystem(oldCompiledJar.toFile(), true);
                 FileSystemUtil.Delegate outFs = FileSystemUtil.getJarFileSystem(linemappedJarDestination.toFile(), true)) {
                remapper.process(inFs.get().getPath("/"), outFs.get().getPath("/"));
            }
        }

        private Collection<Path> getLibraries() {
            return getParameters().getClassPath().getFiles().stream().map(File::toPath).collect(Collectors.toSet());
        }
    }

    public static File getMappedJarFileWithSuffix(String suffix, Path runtimeJar) {
        final String path = runtimeJar.toFile().getAbsolutePath();

        if (!path.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new RuntimeException("Invalid mapped JAR path: " + path);
        }

        return new File(path.substring(0, path.length() - 4) + suffix);
    }

    private Path getMappings() {
        Path inputMappings = WispGradleApiExtension.get(getProject()).getMappingsProvider().get().getClientMappings();

        MemoryMappingTree mappingTree = new MemoryMappingTree();

        try (Reader reader = Files.newBufferedReader(inputMappings, StandardCharsets.UTF_8)) {
            MappingReader.read(reader, new MappingSourceNsSwitch(mappingTree, "named"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read mappings", e);
        }

        final List<MappingsProcessor> mappingsProcessors = new ArrayList<>();

//        MinecraftJarProcessorManager minecraftJarProcessorManager = MinecraftJarProcessorManager.create(getProject());
//
//        if (minecraftJarProcessorManager != null) {
//            mappingsProcessors.add(mappings -> {
//                try (var serviceManager = new ScopedSharedServiceManager()) {
//                    final var configContext = new ConfigContextImpl(getProject(), serviceManager, getExtension());
//                    return minecraftJarProcessorManager.processMappings(mappings, new MappingProcessorContextImpl(configContext));
//                }
//            });
//        }

        if (mappingsProcessors.isEmpty()) {
            return inputMappings;
        }

        boolean transformed = false;

        for (MappingsProcessor mappingsProcessor : mappingsProcessors) {
            if (mappingsProcessor.transform(mappingTree)) {
                transformed = true;
            }
        }

        if (!transformed) {
            return inputMappings;
        }

        final Path outputMappings;

        try {
            outputMappings = Files.createTempFile("loom-transitive-mappings", ".tiny");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp file", e);
        }

        try (Writer writer = Files.newBufferedWriter(outputMappings, StandardCharsets.UTF_8)) {
            Tiny1FileWriter tiny2Writer = new Tiny1FileWriter(writer);
            mappingTree.accept(new MappingSourceNsSwitch(tiny2Writer, "named"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write mappings", e);
        }

        return outputMappings;
    }

    public interface MappingsProcessor {
        boolean transform(MemoryMappingTree mappings);
    }
}