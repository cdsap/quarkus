package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.tasks.worker.CodeGenWorker;
import io.quarkus.runtime.LaunchMode;

@CacheableTask
public abstract class QuarkusGenerateCode extends QuarkusTaskWithConfigurationCache {

    public static final String QUARKUS_GENERATED_SOURCES = "quarkus-generated-sources";
    public static final String QUARKUS_TEST_GENERATED_SOURCES = "quarkus-test-generated-sources";
    // TODO dynamically load generation provider, or make them write code directly in quarkus-generated-sources
    public static final String[] CODE_GENERATION_PROVIDER = new String[] { "grpc", "avdl", "avpr", "avsc" };
    public static final String[] CODE_GENERATION_INPUT = new String[] { "proto", "avro" };

    private Set<Path> sourcesDirectories;
    private FileCollection compileClasspath;

    private final LaunchMode launchMode;
    private final String inputSourceSetName;

    private final Property<ApplicationModel> applicationModel = getProject().getObjects().property(ApplicationModel.class);
    private final Property<String> gradleVersion = getProject().getObjects().property(String.class);
    private final Property<String> finalName = getProject().getObjects().property(String.class);
    private final MapProperty<String, String> effectiveConfigurationValuesProperty = getProject().getObjects().mapProperty(
            String.class,
            String.class);
    private final MapProperty<String, String> cachingRelevantInput = getProject().getObjects().mapProperty(String.class,
            String.class);

    @Inject
    public QuarkusGenerateCode(LaunchMode launchMode, String inputSourceSetName) {
        super("Performs Quarkus pre-build preparations, such as sources generation");
        this.launchMode = launchMode;
        this.inputSourceSetName = inputSourceSetName;
    }

    /**
     * Create a dependency on classpath resolution. This makes sure included build are build this task runs.
     *
     * @return resolved compile classpath
     */
    @CompileClasspath
    public FileCollection getClasspath() {
        return compileClasspath;
    }

    public void setCompileClasspath(Configuration compileClasspath) {
        this.compileClasspath = compileClasspath;
    }

    @Input
    public MapProperty<String, String> getCachingRelevantInput() {
        return cachingRelevantInput;
    }

    @Internal
    public Property<String> getGradleVersion() {
        return gradleVersion;
    }

    @Internal
    public Property<ApplicationModel> getAppModel() {
        return applicationModel;
    }

    @Internal
    public Property<String> getFinalName() {
        return finalName;
    }

    @Internal
    public MapProperty<String, String> getEffectiveConfigurationValues() {
        return effectiveConfigurationValuesProperty;
    }

    @Input
    Map<String, String> getInternalTaskConfig() {
        // Necessary to distinguish the different `quarkusGenerateCode*` tasks, because the task path is _not_
        // an input to the cache key. We need to declare these properties as inputs, because those influence the
        // execution.
        // Documented here: https://docs.gradle.org/current/userguide/build_cache.html#sec:task_output_caching_inputs
        return Map.of("launchMode", launchMode.name(), "inputSourceSetName", inputSourceSetName);
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public Set<File> getInputDirectory() {
        Set<File> inputDirectories = new HashSet<>();

        Path src = projectDir.toPath().resolve("src").resolve(inputSourceSetName);

        for (String input : CODE_GENERATION_INPUT) {
            Path providerSrcDir = src.resolve(input);
            if (Files.exists(providerSrcDir)) {
                inputDirectories.add(providerSrcDir.toFile());
            }
        }

        return inputDirectories;
    }

    @OutputDirectory
    public abstract DirectoryProperty getGeneratedOutputDirectory();

    @TaskAction
    public void generateCode() {
        ApplicationModel appModel = getAppModel().get();
        Map<String, String> values = getEffectiveConfigurationValues().get();

        File outputPath = getGeneratedOutputDirectory().get().getAsFile();

        getLogger().debug("Will trigger preparing sources for source directories: {} buildDir: {}",
                sourcesDirectories, buildDir.getAbsolutePath());

        WorkQueue workQueue = workQueue(values, ArrayList::new);

        workQueue.submit(CodeGenWorker.class, params -> {
            params.getBuildSystemProperties().putAll(values);
            params.getBaseName().set(getFinalName().get());
            params.getTargetDirectory().set(buildDir);
            params.getAppModel().set(appModel);
            params
                    .getSourceDirectories()
                    .setFrom(sourcesDirectories.stream().map(Path::toFile).collect(Collectors.toList()));
            params.getOutputPath().set(outputPath);
            params.getLaunchMode().set(launchMode);
            params.getGradleVersion().set(getGradleVersion().get());
        });

        workQueue.await();

    }

    public void setSourcesDirectories(Set<Path> sourcesDirectories) {
        this.sourcesDirectories = sourcesDirectories;
    }
}
