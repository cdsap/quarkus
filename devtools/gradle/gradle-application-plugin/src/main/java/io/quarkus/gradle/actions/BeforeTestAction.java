package io.quarkus.gradle.actions;

import static io.quarkus.gradle.extension.QuarkusPluginExtension.getLastFile;
import static io.quarkus.runtime.LaunchMode.TEST;

import java.io.File;
import java.nio.file.Files;
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

    private final Provider<RegularFile> pts;

    public BeforeTestAction(File projectDir, FileCollection combinedOutputSourceDirs,
            Provider<File> nativeRunnerPath,
            Provider<RegularFile> applicationModelPath,
            FileCollection mainSourceSetClassesDir,
            Provider<RegularFile> pts) {
        this.projectDir = projectDir;
        this.combinedOutputSourceDirs = combinedOutputSourceDirs;
        this.applicationModelPath = applicationModelPath;
        this.nativeRunnerPath = nativeRunnerPath;
        this.mainSourceSetClassesDir = mainSourceSetClassesDir;
        this.pts = pts;
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
            String content = Files.readString(pts.get().getAsFile().toPath());
            if (!content.isEmpty()) {
                props.put(TEST.getProfileKey(), content);
            }
            props.put(BootstrapConstants.OUTPUT_SOURCES_DIR, outputSourcesDir.toString());

            final File outputDirectoryAsFile = getLastFile(mainSourceSetClassesDir);

            Path projectDirPath = projectDir.toPath();

            // Identify the folder containing the sources associated with this test task
            String fileList = task.getTestClassesDirs().getFiles().stream()
                    .filter(File::exists)
                    .distinct()
                    .map(testSrcDir -> String.format("%s:%s",
                            projectDirPath.relativize(testSrcDir.toPath()),
                            projectDirPath.relativize(outputDirectoryAsFile.toPath())))
                    .collect(Collectors.joining(","));
            task.environment(BootstrapConstants.TEST_TO_MAIN_MAPPINGS, fileList);
            task.getLogger().debug("test dir mapping - {}", fileList);

            String nativeRunner = nativeRunnerPath.get().toPath().toAbsolutePath().toString();
            props.put("native.image.path", nativeRunner);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }
}
