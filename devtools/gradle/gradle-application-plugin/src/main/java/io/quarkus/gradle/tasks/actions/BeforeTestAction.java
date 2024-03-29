package io.quarkus.gradle.tasks.actions;

import static io.quarkus.gradle.extension.QuarkusPluginExtension.getLastFile;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapConstants;

public class BeforeTestAction implements Action<Task> {

    private final File projectDir;
    private final FileCollection combinedOutputSourceDirs;
    private final Provider<RegularFile> applicationModelPath;
    private final Provider<File> nativeRunnerPath;
    private final FileCollection mainSourceSetClassesDir;

    public BeforeTestAction(File projectDir, FileCollection combinedOutputSourceDirs,
            Provider<RegularFile> applicationModelPath, Provider<File> nativeRunnerPath,
            FileCollection mainSourceSetClassesDir) {
        this.projectDir = projectDir;
        this.combinedOutputSourceDirs = combinedOutputSourceDirs;
        this.applicationModelPath = applicationModelPath;
        this.nativeRunnerPath = nativeRunnerPath;
        this.mainSourceSetClassesDir = mainSourceSetClassesDir;
    }

    @Override
    public void execute(Task t) {
        try {
            Test task = (Test) t;
            final Map<String, Object> props = task.getSystemProperties();

            final Path serializedModel = applicationModelPath.get().getAsFile().toPath();
            props.put(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, serializedModel.toString());

            StringJoiner outputSourcesDir = new StringJoiner(",");
            for (File outputSourceDir : combinedOutputSourceDirs.getFiles()) {
                outputSourcesDir.add(outputSourceDir.getAbsolutePath());
            }
            props.put(BootstrapConstants.OUTPUT_SOURCES_DIR, outputSourcesDir.toString());

            final File outputDirectoryAsFile = getLastFile(mainSourceSetClassesDir);

            Path projectDirPath = projectDir.toPath();

            // TODO: Check if we need this sourceSet walk?
            // Identify the folder containing the sources associated with this test task
            //            String fileList = sourceSets.stream()
            //                    .filter(sourceSet -> Objects.equals(
            //                            task.getTestClassesDirs().getAsPath(),
            //                            sourceSet.getOutput().getClassesDirs().getAsPath()))
            //                    .flatMap(sourceSet -> sourceSet.getOutput().getClassesDirs().getFiles().stream())
            //                    .filter(File::exists)
            //                    .distinct()
            //                    .map(testSrcDir -> String.format("%s:%s",
            //                            projectDirPath.relativize(testSrcDir.toPath()),
            //                            projectDirPath.relativize(outputDirectoryAsFile.toPath())))
            //                    .collect(Collectors.joining(","));
            String fileList = task.getTestClassesDirs().getFiles().stream()
                    .filter(File::exists)
                    .distinct()
                    .map(testSrcDir -> String.format("%s:%s",
                            projectDirPath.relativize(testSrcDir.toPath()),
                            projectDirPath.relativize(outputDirectoryAsFile.toPath())))
                    .collect(Collectors.joining(","));
            task.environment(BootstrapConstants.TEST_TO_MAIN_MAPPINGS, fileList);
            task.getLogger().debug("test dir mapping - {}", fileList);

            //            QuarkusBuild quarkusBuild = project.getTasks().named(QuarkusPlugin.QUARKUS_BUILD_TASK_NAME, QuarkusBuild.class)
            //                    .get();
            String nativeRunner = nativeRunnerPath.get().toPath().toAbsolutePath().toString();
            System.out.println("nativeRunner: " + nativeRunner);
            props.put("native.image.path", nativeRunner);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }
}
