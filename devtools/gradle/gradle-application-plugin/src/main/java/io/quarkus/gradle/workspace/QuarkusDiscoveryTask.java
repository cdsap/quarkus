package io.quarkus.gradle.workspace;

import java.io.*;
import java.util.*;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.workspace.descriptors.DefaultProjectDescriptor;

@DisableCachingByDefault(because = "Currently not cached in the build cache, " +
        "since DefaultProjectDescriptor contains absolute paths and we output absolute paths")
public abstract class QuarkusDiscoveryTask extends DefaultTask {

    private final String projectPath;
    private final String projectName;

    @Input
    public abstract Property<DefaultProjectDescriptor> getProjectDescriptor();

    @OutputFile
    public abstract RegularFileProperty getProjectDescriptorOutput();

    public QuarkusDiscoveryTask() {
        this.projectPath = getProject().getPath();
        this.projectName = getProject().getName();
    }

    @TaskAction
    public void run() throws IOException {
        DefaultProjectDescriptor projectDescriptor = getProjectDescriptor().get();
        List<Map.Entry<String, String>> lines = new ArrayList<>();
        lines.add(entry("project.name", projectName));
        lines.add(entry("project.path", projectPath));
        lines.add(entry("project.buildDir", projectDescriptor.getBuildDir().getAbsolutePath()));
        lines.add(entry("project.projectDir", projectDescriptor.getProjectDir().getAbsolutePath()));
        lines.add(entry("project.buildFile", projectDescriptor.getBuildFile().getAbsolutePath()));
        projectDescriptor.getSourceSetTasks().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(sourceSet -> lines.add(entry("sourceSet." + sourceSet.getKey() + ".tasks",
                        String.join(",", new TreeSet<>(sourceSet.getValue())))));
        projectDescriptor.getTasks().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(task -> {
                    lines.add(entry("task." + task.getKey() + ".source", task.getValue().getSourceDir().getAbsolutePath()));
                    lines.add(entry("task." + task.getKey() + ".destinationDirectory",
                            task.getValue().getDestinationDir().getAbsolutePath()));
                    lines.add(entry("task." + task.getKey() + ".type",
                            task.getValue().getTaskType().name()));
                });
        writeLines(getProjectDescriptorOutput().get().getAsFile(), lines);
    }

    private void writeLines(File file, List<Map.Entry<String, String>> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (Map.Entry<String, String> line : lines) {
                writer.write(line.getKey() + "=" + line.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map.Entry<String, String> entry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
