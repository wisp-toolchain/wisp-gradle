package me.alphamode.wisp.test;

import me.alphamode.wisp.WispExtensionApiImpl;
import me.alphamode.wisp.WispGradle;
import org.gradle.api.Describable;
import org.gradle.api.Project;
import org.gradle.api.reflect.ObjectInstantiationException;
import org.gradle.internal.extensibility.DefaultConvention;
import org.gradle.internal.instantiation.InstanceGenerator;

public class TestExtension extends DefaultConvention {
    public TestExtension(Project project, WispGradle wispGradle) {
        super(new InstanceGenerator() {
            @Override
            public <T> T newInstanceWithDisplayName(Class<? extends T> type, Describable displayName, Object... parameters) throws ObjectInstantiationException {
                return (T) new WispExtensionApiImpl(project, wispGradle);
            }

            @Override
            public <T> T newInstance(Class<? extends T> type, Object... parameters) throws ObjectInstantiationException {
                return (T) new WispExtensionApiImpl(project, wispGradle);
            }
        });
    }
}
