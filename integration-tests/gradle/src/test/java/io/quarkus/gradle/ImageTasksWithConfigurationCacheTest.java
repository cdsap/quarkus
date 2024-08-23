package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ImageTasksWithConfigurationCacheTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldReuseConfigurationCacheImageBuildIfTheExtensionIsAdded() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");

        runGradleWrapper(projectDir, "addExtension", "--extensions=quarkus-container-image-docker");

        BuildResult buildResult = runGradleWrapper(projectDir, "imageBuild");
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":imageBuild"))).isTrue();

        assertTrue(buildResult.getOutput().contains("Configuration cache entry stored"));
        BuildResult buildResult3 = runGradleWrapper(projectDir, "imageBuild");
        assertTrue(buildResult3.getOutput().contains("Reusing configuration cache."));

    }

    @Test
    public void shouldReuseConfigurationCacheWithProjectIsolationImageBuildIfTheExtensionIsAdded() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");

        runGradleWrapper(projectDir, "addExtension", "--extensions=quarkus-container-image-docker");

        BuildResult buildResult = runGradleWrapper(projectDir, "imageBuild", "-Dorg.gradle.unsafe.isolated-projects=true");
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":imageBuild"))).isTrue();

        assertTrue(buildResult.getOutput().contains("Configuration cache entry stored"));
        BuildResult buildResult3 = runGradleWrapper(projectDir, "imageBuild", "-Dorg.gradle.unsafe.isolated-projects=true");
        assertTrue(buildResult3.getOutput().contains("Reusing configuration cache."));

    }

    @Test
    public void shouldFailIfExtensionIsNotDefinedInTheBuild() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");
        BuildResult buildResultImageBuild = runGradleWrapper(true, projectDir, "clean", "imageBuild", "--no-build-cache");
        assertTrue(buildResultImageBuild.getOutput()
                .contains("Task: quarkusImageExtensionChecks requires extensions: quarkus-container-image-docker"));

        BuildResult buildResultImagePush = runGradleWrapper(true, projectDir, "clean", "imagePush", "--no-build-cache");
        assertTrue(buildResultImagePush.getOutput()
                .contains("Task: quarkusImageExtensionChecks requires extensions: quarkus-container-image-docker"));

    }
}
