package io.quarkus.gradle.workspace.descriptors;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Set;

public class PropertiesBasedProjectDescriptor implements ProjectDescriptor {

    private final Properties properties;

    public PropertiesBasedProjectDescriptor(File projectDescriptor) {
        this.properties = new Properties();
        try {
            properties.load(Files.newInputStream(projectDescriptor.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getProjectPath() {
        return properties.getProperty("project.path");
    }

    @Override
    public File getProjectDir() {
        return new File(properties.getProperty("project.projectDir"));
    }

    @Override
    public File getBuildDir() {
        return new File(properties.getProperty("project.buildDir"));
    }

    @Override
    public File getBuildFile() {
        return new File(properties.getProperty("project.buildFile"));
    }

    @Override
    public Set<String> getTasksForSourceSet(String sourceName) {
        return properties.getProperty("sourceSet." + sourceName + ".tasks", "").isEmpty()
                ? Set.of()
                : Set.of(properties.getProperty("sourceSet." + sourceName + ".tasks").split(","));
    }

    @Override
    public String getTaskSource(String task) {
        return properties.getProperty("task." + task + ".source");
    }

    @Override
    public String getTaskDestinationDir(String task) {
        return properties.getProperty("task." + task + ".destinationDirectory");
    }

    @Override
    public TaskType getTaskType(String task) {
        return properties.getProperty("task." + task + ".type", "COMPILE").equals("COMPILE")
                ? TaskType.COMPILE
                : TaskType.RESOURCES;
    }

    @Override
    public String toString() {
        return "PropertiesBasedProjectDescriptor{" +
                "properties=" + properties +
                '}';
    }
}
