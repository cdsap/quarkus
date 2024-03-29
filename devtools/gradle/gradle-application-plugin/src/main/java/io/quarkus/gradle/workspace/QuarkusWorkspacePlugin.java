package io.quarkus.gradle.workspace;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

/**
 * Applies {@link QuarkusProjectDiscoveryPlugin} to all projects.
 * Should not contain any other logic than that here.
 */
public class QuarkusWorkspacePlugin implements Plugin<Settings> {

    @Override
    @SuppressWarnings("Convert2Lambda")
    public void apply(Settings target) {
        target.getGradle().afterProject(new Action<>() {
            @Override
            public void execute(Project project) {
                if (project.getParent() != null) {
                    // Not a root project
                    return;
                }
                project.allprojects(new Action<>() {
                    @Override
                    public void execute(Project project) {
                        // Do not apply any other logic here,
                        // since it will be applied to all projects and will be problematic later
                        // with Gradle isolated-projects
                        project.getPluginManager().apply(QuarkusProjectDiscoveryPlugin.class);
                    }
                });
            }
        });
    }
}