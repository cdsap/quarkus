package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.QuarkusPlugin.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TasksConfigurationCacheCompatibilityTest {

    @TempDir
    Path testProjectDir;

    private static Stream<String> compatibleTasks() {
        return Stream.of(
                QUARKUS_GENERATE_CODE_TASK_NAME,
                QUARKUS_GENERATE_CODE_DEV_TASK_NAME,
                QUARKUS_BUILD_DEP_TASK_NAME,
                QUARKUS_BUILD_APP_PARTS_TASK_NAME,
                QUARKUS_SHOW_EFFECTIVE_CONFIG_TASK_NAME,
                QUARKUS_BUILD_TASK_NAME,
                QUARKUS_GENERATE_CODE_TESTS_TASK_NAME);
    }

    private static Stream<String> nonCompatibleQuarkusBuildTasks() {
        return Stream.of(
                QUARKUS_RUN_TASK_NAME,
                IMAGE_BUILD_TASK_NAME,
                IMAGE_PUSH_TASK_NAME,
                DEPLOY_TASK_NAME);
    }


    @ParameterizedTest
    @MethodSource("compatibleTasks")
    public void configurationCacheIsReusedTest(String taskName) throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        buildResult(":help", "--configuration-cache");

        BuildResult firstBuild = buildResult(taskName, "--configuration-cache");
        assertTrue(firstBuild.getOutput().contains("Configuration cache entry stored"));

        BuildResult secondBuild = buildResult(taskName, "--configuration-cache");
        assertTrue(secondBuild.getOutput().contains("Reusing configuration cache."));
    }

    @ParameterizedTest
    @MethodSource("compatibleTasks")
    public void configurationCacheIsReusedWhenProjectIsolationIsUsedTest(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult firstBuild = buildResult(taskName, "-Dorg.gradle.unsafe.isolated-projects=true");
        assertTrue(firstBuild.getOutput().contains("Configuration cache entry stored"));

        BuildResult secondBuild = buildResult(taskName, "-Dorg.gradle.unsafe.isolated-projects=true");
        System.out.println(secondBuild.getOutput());
        assertTrue(secondBuild.getOutput().contains("Reusing configuration cache."));
    }

    @ParameterizedTest
    @MethodSource("nonCompatibleQuarkusBuildTasks")
    public void quarkusBuildTasksNonCompatibleWithConfigurationCacheNotFail(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult build = buildResult(taskName);
        assertTrue(build.getOutput().contains("BUILD SUCCESSFUL"));

    }

    @ParameterizedTest
    @MethodSource("nonCompatibleQuarkusBuildTasks")
    public void quarkusBuildTasksNonCompatibleWithConfigurationCacheNotFailWhenUsingConfigurationCache(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult build = buildResult(taskName, "--no-configuration-cache");
        assertTrue(build.getOutput().contains("BUILD SUCCESSFUL"));

    }

    private BuildResult buildResult(String task, String configurationCacheCommand) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(task, "--info", "--stacktrace", "--build-cache", configurationCacheCommand)
                .build();
    }

    private BuildResult buildResult(String task) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(task, "--info", "--stacktrace", "--build-cache")
                .build();
    }
}
