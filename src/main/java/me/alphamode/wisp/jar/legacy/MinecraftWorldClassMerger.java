package me.alphamode.wisp.jar.legacy;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public class MinecraftWorldClassMerger {
    private static final String SIDE_DESCRIPTOR = "Lme/alphamode/wisp/env/Environment;";
    private static final String ITF_DESCRIPTOR = "Lme/alphamode/wisp/env/OnlyInInterfaces;";
    private static final String ITF_LIST_DESCRIPTOR = "Lnet/fabricmc/api/EnvironmentInterfaces;";
    private static final String SIDED_DESCRIPTOR = "Lme/alphamode/wisp/env/OnlyIn;";

    private abstract static class Merger<T> {
        private final Map<String, T> entriesClient, entriesServer;
        private final List<String> entryNames;

        Merger(List<T> entriesClient, List<T> entriesServer) {
            this.entriesClient = new LinkedHashMap<>();
            this.entriesServer = new LinkedHashMap<>();

            List<String> listClient = toMap(entriesClient, this.entriesClient);
            List<String> listServer = toMap(entriesServer, this.entriesServer);

            this.entryNames = mergePreserveOrder(listClient, listServer);
        }

        public abstract String getName(T entry);

        public abstract void applySide(T entry, String side);

        private List<String> toMap(List<T> entries, Map<String, T> map) {
            List<String> list = new ArrayList<>(entries.size());

            for (T entry : entries) {
                String name = getName(entry);
                map.put(name, entry);
                list.add(name);
            }

            return list;
        }

        public void merge(List<T> list) {
            for (String s : entryNames) {
                T entryClient = entriesClient.get(s);
                T entryServer = entriesServer.get(s);

                if (entryClient != null && entryServer != null) {
                    list.add(entryClient);
                } else if (entryClient != null) {
                    applySide(entryClient, "CLIENT");
                    list.add(entryClient);
                } else {
                    applySide(entryServer, "SERVER");
                    list.add(entryServer);
                }
            }
        }
    }

    private static void visitSideAnnotation(AnnotationVisitor av, String side) {
        av.visitEnum("value", SIDE_DESCRIPTOR, side.toUpperCase(Locale.ROOT));
        av.visitEnd();
    }

    private static void visitItfAnnotation(AnnotationVisitor av, String side, List<String> itfDescriptors) {
        for (String itf : itfDescriptors) {
            AnnotationVisitor avItf = av.visitAnnotation(null, ITF_DESCRIPTOR);
            avItf.visitEnum("value", SIDE_DESCRIPTOR, side.toUpperCase(Locale.ROOT));
            avItf.visit("itf", Type.getType("L" + itf + ";"));
            avItf.visitEnd();
        }
    }

    public static class SidedClassVisitor extends ClassVisitor {
        private final String side;

        public SidedClassVisitor(int api, ClassVisitor cv, String side) {
            super(api, cv);
            this.side = side;
        }

        @Override
        public void visitEnd() {
            AnnotationVisitor av = cv.visitAnnotation(SIDED_DESCRIPTOR, true);
            visitSideAnnotation(av, side);
            super.visitEnd();
        }
    }

    public MinecraftWorldClassMerger() {
    }

    public byte[] mergeWorld(byte[] classClient, byte[] classServer) {
        ClassReader readerC = new ClassReader(classClient);
        ClassReader readerS = new ClassReader(classServer);
        ClassWriter writer = new ClassWriter(0);

        ClassNode nodeC = new ClassNode(Opcodes.ASM9);
        readerC.accept(nodeC, 0);

        ClassNode nodeS = new ClassNode(Opcodes.ASM9);
        readerS.accept(nodeS, 0);

        ClassNode nodeOut = new ClassNode(Opcodes.ASM9);
        nodeOut.version = nodeC.version;
        nodeOut.access = nodeC.access;
        nodeOut.name = nodeC.name;
        nodeOut.signature = nodeC.signature;
        nodeOut.superName = nodeC.superName;
        nodeOut.sourceFile = nodeC.sourceFile;
        nodeOut.sourceDebug = nodeC.sourceDebug;
        nodeOut.outerClass = nodeC.outerClass;
        nodeOut.outerMethod = nodeC.outerMethod;
        nodeOut.outerMethodDesc = nodeC.outerMethodDesc;
        nodeOut.module = nodeC.module;
        nodeOut.nestHostClass = nodeC.nestHostClass;
        nodeOut.nestMembers = nodeC.nestMembers;
        nodeOut.attrs = nodeC.attrs;

        if (nodeC.invisibleAnnotations != null) {
            nodeOut.invisibleAnnotations = new ArrayList<>();
            nodeOut.invisibleAnnotations.addAll(nodeC.invisibleAnnotations);
        }

        if (nodeC.invisibleTypeAnnotations != null) {
            nodeOut.invisibleTypeAnnotations = new ArrayList<>();
            nodeOut.invisibleTypeAnnotations.addAll(nodeC.invisibleTypeAnnotations);
        }

        if (nodeC.visibleAnnotations != null) {
            nodeOut.visibleAnnotations = new ArrayList<>();
            nodeOut.visibleAnnotations.addAll(nodeC.visibleAnnotations);
        }

        if (nodeC.visibleTypeAnnotations != null) {
            nodeOut.visibleTypeAnnotations = new ArrayList<>();
            nodeOut.visibleTypeAnnotations.addAll(nodeC.visibleTypeAnnotations);
        }

        List<String> itfs = mergePreserveOrder(nodeC.interfaces, nodeS.interfaces);
        nodeOut.interfaces = new ArrayList<>();

        List<String> clientItfs = new ArrayList<>();
        List<String> serverItfs = new ArrayList<>();

        for (String s : itfs) {
            boolean nc = nodeC.interfaces.contains(s);
            boolean ns = nodeS.interfaces.contains(s);
            nodeOut.interfaces.add(s);

            if (nc && !ns) {
                clientItfs.add(s);
            } else if (ns && !nc) {
                serverItfs.add(s);
            }
        }

        if (!clientItfs.isEmpty() || !serverItfs.isEmpty()) {
            AnnotationVisitor envInterfaces = nodeOut.visitAnnotation(ITF_LIST_DESCRIPTOR, false);
            AnnotationVisitor eiArray = envInterfaces.visitArray("value");

            if (!clientItfs.isEmpty()) {
                visitItfAnnotation(eiArray, "CLIENT", clientItfs);
            }

            if (!serverItfs.isEmpty()) {
                visitItfAnnotation(eiArray, "SERVER", serverItfs);
            }

            eiArray.visitEnd();
            envInterfaces.visitEnd();
        }

        new Merger<>(nodeC.innerClasses, nodeS.innerClasses) {
            @Override
            public String getName(InnerClassNode entry) {
                return entry.name;
            }

            @Override
            public void applySide(InnerClassNode entry, String side) {
            }
        }.merge(nodeOut.innerClasses);

        new Merger<>(nodeC.fields, nodeS.fields) {
            @Override
            public String getName(FieldNode entry) {
                return entry.name + ";;" + entry.desc;
            }

            @Override
            public void applySide(FieldNode entry, String side) {
                AnnotationVisitor av = entry.visitAnnotation(SIDED_DESCRIPTOR, false);
                visitSideAnnotation(av, side);
            }
        }.merge(nodeOut.fields);

        new Merger<>(nodeC.methods, nodeS.methods) {
            @Override
            public String getName(MethodNode entry) {
                return entry.name + entry.desc;
            }

            @Override
            public void applySide(MethodNode entry, String side) {
                AnnotationVisitor av = entry.visitAnnotation(SIDED_DESCRIPTOR, false);
                visitSideAnnotation(av, side);
            }
        }.merge(nodeOut.methods);

        nodeOut.accept(writer);
        return writer.toByteArray();
    }

    private static List<String> mergePreserveOrder(List<String> first, List<String> second) {
        List<String> out = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < first.size() || j < second.size()) {
            while (i < first.size() && j < second.size()
                    && first.get(i).equals(second.get(j))) {
                out.add(first.get(i));
                i++;
                j++;
            }

            while (i < first.size() && !second.contains(first.get(i))) {
                out.add(first.get(i));
                i++;
            }

            while (j < second.size() && !first.contains(second.get(j))) {
                out.add(second.get(j));
                j++;
            }
        }

        return out;
    }
}
