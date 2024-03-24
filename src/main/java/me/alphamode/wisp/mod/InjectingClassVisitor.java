package me.alphamode.wisp.mod;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class InjectingClassVisitor extends ClassVisitor {
    private static final int INTERFACE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE;

    private final List<InjectedInterface> injectedInterfaces;
    private final Set<String> knownInnerClasses = new HashSet<>();

    InjectingClassVisitor(int asmVersion, ClassWriter writer, List<InjectedInterface> injectedInterfaces) {
        super(asmVersion, writer);
        this.injectedInterfaces = injectedInterfaces;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        Set<String> modifiedInterfaces = new LinkedHashSet<>(interfaces.length + injectedInterfaces.size());
        Collections.addAll(modifiedInterfaces, interfaces);

        for (InjectedInterface injectedInterface : injectedInterfaces) {
            modifiedInterfaces.add(injectedInterface.ifaceName());
        }

        // See JVMS: https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-ClassSignature
        if (signature != null) {
            var resultingSignature = new StringBuilder(signature);

            for (InjectedInterface injectedInterface : injectedInterfaces) {
                String superinterfaceSignature = "L" + injectedInterface.ifaceName() + ";";

                if (resultingSignature.indexOf(superinterfaceSignature) == -1) {
                    resultingSignature.append(superinterfaceSignature);
                }
            }

            signature = resultingSignature.toString();
        }

        super.visit(version, access, name, signature, superName, modifiedInterfaces.toArray(new String[0]));
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        this.knownInnerClasses.add(name);
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitEnd() {
        // inject any necessary inner class entries
        // this may produce technically incorrect bytecode cuz we don't know the actual access flags for inner class entries
        // but it's hopefully enough to quiet some IDE errors
        for (final InjectedInterface itf : injectedInterfaces) {
            if (this.knownInnerClasses.contains(itf.ifaceName())) {
                continue;
            }

            int simpleNameIdx = itf.ifaceName().lastIndexOf('/');
            final String simpleName = simpleNameIdx == -1 ? itf.ifaceName() : itf.ifaceName().substring(simpleNameIdx + 1);
            int lastIdx = -1;
            int dollarIdx = -1;

            // Iterate through inner class entries starting from outermost to innermost
            while ((dollarIdx = simpleName.indexOf('$', dollarIdx + 1)) != -1) {
                if (dollarIdx - lastIdx == 1) {
                    continue;
                }

                // Emit the inner class entry from this to the last one
                if (lastIdx != -1) {
                    final String outerName = itf.ifaceName().substring(0, simpleNameIdx + 1 + lastIdx);
                    final String innerName = simpleName.substring(lastIdx + 1, dollarIdx);
                    super.visitInnerClass(outerName + '$' + innerName, outerName, innerName, INTERFACE_ACCESS);
                }

                lastIdx = dollarIdx;
            }

            // If we have a trailer to append
            if (lastIdx != -1 && lastIdx != simpleName.length()) {
                final String outerName = itf.ifaceName().substring(0, simpleNameIdx + 1 + lastIdx);
                final String innerName = simpleName.substring(lastIdx + 1);
                super.visitInnerClass(outerName + '$' + innerName, outerName, innerName, INTERFACE_ACCESS);
            }
        }

        super.visitEnd();
    }
}