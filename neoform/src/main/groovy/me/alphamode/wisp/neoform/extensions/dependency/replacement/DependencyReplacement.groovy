package me.alphamode.wisp.neoform.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.annotations.NotNull

import java.util.function.Consumer

/**
 * Defines an extension which handles the dependency replacements.
 */
@CompileStatic
interface DependencyReplacement extends BaseDSLElement<DependencyReplacement> {


    void handleConfiguration(Configuration configuration);

    /**
     * The dependency replacement handlers.
     *
     * @return The handlers.
     */
    @NotNull
    @DSLProperty
    NamedDomainObjectContainer<DependencyReplacementHandler> getReplacementHandlers();

    void afterDefinitionBake(Consumer<Project> callback);
}