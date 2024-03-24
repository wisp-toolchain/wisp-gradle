package me.alphamode.wisp.neoform;

import me.alphamode.wisp.neoform.dependency.NeoFormDependencyManager;
import me.alphamode.wisp.neoform.extensions.NeoFormRuntimeExtension;
import me.alphamode.wisp.neoform.naming.NeoFormOfficialNamingChannelConfigurator;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

public class NeoFormPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        NeoFormRuntimeExtension runtimeExtension = project.getExtensions().create("neoFormRuntime", NeoFormRuntimeExtension.class, project);

        NeoFormOfficialNamingChannelConfigurator.getInstance().configure(project);

        //Setup handling of the dependencies
        NeoFormDependencyManager.getInstance().apply(project);

        //Add Known repos
        project.getRepositories().maven(e -> {
            e.setUrl(UrlConstants.NEO_FORGE_MAVEN);
            e.metadataSources(MavenArtifactRepository.MetadataSources::mavenPom);
        });
    }
}
