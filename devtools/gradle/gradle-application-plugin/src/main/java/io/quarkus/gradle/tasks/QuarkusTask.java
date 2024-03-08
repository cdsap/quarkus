package io.quarkus.gradle.tasks;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

public abstract class QuarkusTask extends DefaultQuarkusTask {

    private final transient QuarkusPluginExtension extension;

    QuarkusTask(String description) {
        super(description);
        this.extension = getProject().getExtensions().findByType(QuarkusPluginExtension.class);
        // Calling this method tells Gradle that it should not fail the build. Side effect is that the configuration
        // cache will be at least degraded, but the build will not fail.
        notCompatibleWithConfigurationCache("The Quarkus Plugin isn't compatible with the configuration cache");
    }

    QuarkusPluginExtension extension() {
        return extension;
    }
}
