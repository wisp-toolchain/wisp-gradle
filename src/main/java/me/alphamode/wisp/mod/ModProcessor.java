package me.alphamode.wisp.mod;

import me.alphamode.wisp.util.ZipUtils;
import net.fabricmc.tinyremapper.extension.mixin.common.data.Pair;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModProcessor {
    private final List<ModDefinition> mods = new ArrayList<>();
    private static final Map<String, ModDefinitionFactory> FACTORIES = Map.of(
            "wisp.mod.toml", WispModDefinition::new,
            "fabric.mod.json",  file -> new FabricModDefinition()
    );


    public void calculateMods(Project project) {
        for (SourceSet set : project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()) {
            set.getResources().forEach(file -> {
                if (file.isFile() && FACTORIES.containsKey(file.getName())) {
                    var mod = FACTORIES.get(file.getName()).create(file);
                    if (mod != null)
                        mods.add(mod);
                }
            });
        }
    }

    private List<Pair<String, ZipUtils.UnsafeUnaryOperator<byte[]>>> getTransformers(List<InjectedInterface> injectedInterfaces) {
        return injectedInterfaces.stream()
                .collect(Collectors.groupingBy(InjectedInterface::className))
                .entrySet()
                .stream()
                .map(entry -> {
                    final String zipEntry = entry.getKey().replaceAll("\\.", "/") + ".class";
                    return Pair.of(zipEntry, getTransformer(entry.getValue()));
                }).toList();
    }

    private ZipUtils.UnsafeUnaryOperator<byte[]> getTransformer(List<InjectedInterface> injectedInterfaces) {
        return input -> {
            final ClassReader reader = new ClassReader(input);
            final ClassWriter writer = new ClassWriter(0);
            final ClassVisitor classVisitor = new InjectingClassVisitor(Opcodes.ASM9, writer, injectedInterfaces);
            reader.accept(classVisitor, 0);
            return writer.toByteArray();
        };
    }

    public void process(Path jar) {
        List<InjectedInterface> interfaces = new ArrayList<>();
        for (ModDefinition modDefinition : mods) {
            System.out.println(modDefinition);
            interfaces.addAll(modDefinition.getInjectedInterfaces());
        }
        ZipUtils.transform(jar, getTransformers(interfaces));
    }

    public List<ModDefinition> getMods() {
        return mods;
    }

    interface ModDefinitionFactory {
        ModDefinition create(File file);
    }
}
