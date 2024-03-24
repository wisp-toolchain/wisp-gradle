package me.alphamode.wisp.neoform.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.tasks.Internal

/**
 * Interface that indicates that a given task has a project associated with it.
 */
@CompileStatic
trait WithProject implements WithOperations {
    /**
     * The project for the object.
     *
     * @return The project for the task.
     */
    @Internal
    @ProjectGetter
    abstract Project getProject();
}