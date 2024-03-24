package me.alphamode.wisp.tasks;

import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Collectors;

public class RunTask extends JavaExec {
    @Inject
    public RunTask() {

    }
}
