package io.quarkus.gradle.tasks;

public abstract class QuarkusTaskWithConfigurationCache extends DefaultQuarkusTask {

    QuarkusTaskWithConfigurationCache(String description) {
        super(description);
    }
}
