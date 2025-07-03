package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.QuarkusPlugin.BUILD_NATIVE_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.TEST_NATIVE_TASK_NAME;
import static io.quarkus.gradle.tasks.AbstractQuarkusExtension.*;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.process.JavaForkOptions;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

public abstract class EffectiveConfigExtension {

    @Inject
    public EffectiveConfigExtension(Project project, QuarkusPluginExtension extension) {
        project.getGradle().getTaskGraph().whenReady(taskGraph -> {
            if (taskGraph.hasTask(project.getPath() + BUILD_NATIVE_TASK_NAME)
                    || taskGraph.hasTask(project.getPath() + TEST_NATIVE_TASK_NAME)) {
                getNativeBuild().set(true);
            } else {
                getNativeBuild().set(false);
            }
        });
        Map<String, Object> projectProperties = new HashMap<>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if ((entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus."))) {
                projectProperties.put(entry.getKey(), entry.getValue());
            }
        }
        getProjectProperties().set(projectProperties);
        getFinalName().set(extension.getFinalName());
        getIgnoredEntries().set(extension.ignoredEntriesProperty());
        getForcedProperties().set(extension.forcedPropertiesProperty());
        getCodeGenForkOptions().set(getProviderFactory().provider(() -> extension.codeGenForkOptions));
        getMainResources().setFrom(project.getExtensions().getByType(SourceSetContainer.class).getByName(MAIN_SOURCE_SET_NAME)
                .getResources().getSourceDirectories());
        getQuarkusBuildProperties().set(extension.getQuarkusBuildProperties());
        getQuarkusRelevantProjectProperties().set(getQuarkusRelevantProjectProperties(project));
        getQuarkusProfileSystemVariable().set(getProviderFactory().systemProperty(QUARKUS_PROFILE));
        getQuarkusProfileEnvVariable().set(getProviderFactory().environmentVariable("QUARKUS_PROFILE"));

    }

    @Inject
    public abstract ProviderFactory getProviderFactory();

    @Input
    public abstract MapProperty<String, Object> getProjectProperties();

    @Input
    public abstract Property<String> getFinalName();

    @Input
    public abstract ListProperty<String> getIgnoredEntries();

    @Input
    @Optional
    public abstract MapProperty<String, String> getForcedProperties();

    @Nested
    public abstract ListProperty<Action<? super JavaForkOptions>> getCodeGenForkOptions();

    @Internal
    public abstract ConfigurableFileCollection getMainResources();

    @Input
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    @Input
    public abstract MapProperty<String, String> getQuarkusRelevantProjectProperties();

    @Input
    @Optional
    public abstract MapProperty<String, Object> getManifestAttributes();

    @Input
    @Optional
    public abstract MapProperty<String, Attributes> getManifestSections();

    @Input
    @Optional
    public abstract Property<Boolean> getNativeBuild();

    @Input
    @Optional
    public abstract Property<String> getQuarkusProfileSystemVariable();

    @Input
    @Optional
    public abstract Property<String> getQuarkusProfileEnvVariable();

    private Provider<Map<String, String>> getQuarkusRelevantProjectProperties(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("8.0")) >= 0) {
            // This is more efficient, i.e.: configuration cache is invalidated only when quarkus properties change
            return getProviderFactory().gradlePropertiesPrefixedBy("quarkus.");
        } else {
            return getProviderFactory().provider(() -> project.getProperties().entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> Map.entry(e.getKey(), e.getValue().toString()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

}
