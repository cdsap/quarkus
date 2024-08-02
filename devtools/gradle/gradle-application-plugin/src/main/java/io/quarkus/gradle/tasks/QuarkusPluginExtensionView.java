package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.AbstractQuarkusExtension.*;
import static io.smallrye.common.expression.Expression.Flag.*;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.*;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;
import org.gradle.process.JavaForkOptions;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.expression.Expression;

/**
 * Configuration cache compatible view of Quarkus extension
 */
public abstract class QuarkusPluginExtensionView {

    @Inject
    public QuarkusPluginExtensionView(Project project, QuarkusPluginExtension extension) {
        getCacheLargeArtifacts().set(extension.getCacheLargeArtifacts());
        getCleanupBuildOutput().set(extension.getCleanupBuildOutput());
        getFinalName().set(extension.getFinalName());
        getCodeGenForkOptions().set(getProviderFactory().provider(() -> extension.codeGenForkOptions));
        getIgnoredEntries().set(extension.ignoredEntriesProperty());
        getMainResources().setFrom(project.getExtensions().getByType(SourceSetContainer.class).getByName(MAIN_SOURCE_SET_NAME)
                .getResources().getSourceDirectories());
        getQuarkusBuildProperties().set(extension.getQuarkusBuildProperties());
        getQuarkusRelevantProjectProperties().set(getQuarkusRelevantProjectProperties(project));
        getQuarkusProfileSystemVariable().set(getProviderFactory().systemProperty(QUARKUS_PROFILE));
        getQuarkusProfileEnvVariable().set(getProviderFactory().environmentVariable("QUARKUS_PROFILE"));
        getCachingRelevantInput()
                .set(extension.baseConfig().cachingRelevantProperties(extension.getCachingRelevantProperties().get()));
        getForcedProperties().set(extension.forcedPropertiesProperty());
        Map<String, Object> projectProperties = new HashMap<>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if ((entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus."))) {
                projectProperties.put(entry.getKey(), entry.getValue());
            }
        }
        getProjectProperties().set(projectProperties);
        getJarEnabled().set(extension.baseConfig().packageConfig().jar().enabled());
        getManifestAttributes().set(extension.manifest().getAttributes());
        getManifestSections().set(extension.manifest().getSections());
        getNativeEnabled().set(extension.baseConfig().nativeConfig().enabled());
        getNativeSourcesOnly().set(extension.baseConfig().nativeConfig().sourcesOnly());

    }

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

    @Inject
    public abstract ProviderFactory getProviderFactory();

    @Input
    public abstract Property<Boolean> getCacheLargeArtifacts();

    @Input
    public abstract Property<Boolean> getCleanupBuildOutput();

    @Input
    public abstract Property<String> getFinalName();

    @Input
    public abstract MapProperty<String, Object> getProjectProperties();

    @Nested
    public abstract ListProperty<Action<? super JavaForkOptions>> getCodeGenForkOptions();

    @Input
    @Optional
    public abstract Property<Boolean> getJarEnabled();

    @Input
    @Optional
    public abstract Property<Boolean> getNativeEnabled();

    @Input
    @Optional
    public abstract Property<Manifest> getManifest();

    @Input
    @Optional
    public abstract Property<Boolean> getNativeSourcesOnly();

    @Input
    public abstract ListProperty<String> getIgnoredEntries();

    @Input
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    @Input
    public abstract MapProperty<String, String> getQuarkusRelevantProjectProperties();

    @Internal
    public abstract ConfigurableFileCollection getMainResources();

    @Input
    @Optional
    public abstract Property<String> getQuarkusProfileSystemVariable();

    @Input
    @Optional
    public abstract Property<String> getQuarkusProfileEnvVariable();

    @Input
    @Optional
    public abstract MapProperty<String, String> getCachingRelevantInput();

    @Input
    @Optional
    public abstract MapProperty<String, String> getForcedProperties();

    @Input
    @Optional
    public abstract MapProperty<String, Object> getManifestAttributes();

    @Input
    @Optional
    public abstract MapProperty<String, Attributes> getManifestSections();

    /**
     * TODO: Move out of this class?
     */

    private void exportCustomManifestProperties(Map<String, Object> properties) {
        for (Map.Entry<String, Object> attribute : getManifestAttributes().get().entrySet()) {
            properties.put(toManifestAttributeKey(attribute.getKey()),
                    attribute.getValue());
        }

        for (Map.Entry<String, Attributes> section : getManifestSections().get().entrySet()) {
            for (Map.Entry<String, Object> attribute : section.getValue().entrySet()) {
                properties
                        .put(toManifestSectionAttributeKey(section.getKey(), attribute.getKey()), attribute.getValue());
            }
        }
    }

    protected EffectiveConfig buildEffectiveConfiguration(ResolvedDependency appArtifact) {
        Map<String, Object> properties = new HashMap<>();
        exportCustomManifestProperties(properties);

        Map<String, String> defaultProperties = new HashMap<>();
        String userIgnoredEntries = String.join(",", getIgnoredEntries().get());
        if (!userIgnoredEntries.isEmpty()) {
            defaultProperties.put("quarkus.package.jar.user-configured-ignored-entries", userIgnoredEntries);
        }
        Set<File> resourcesDirs = getMainResources().getFiles();
        defaultProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        defaultProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        Map<String, String> mutatedForcedProperties = new HashMap<>(getForcedProperties().get());
        getProjectProperties().get().forEach((k, v) -> {
            mutatedForcedProperties.put(k, v.toString());

        });
        return EffectiveConfig.builder()
                .withForcedProperties(mutatedForcedProperties)
                .withTaskProperties(properties)
                .withBuildProperties(getQuarkusBuildProperties().get())
                .withProjectProperties(getQuarkusRelevantProjectProperties().get())
                .withDefaultProperties(defaultProperties)
                .withSourceDirectories(resourcesDirs)
                .withProfile(getQuarkusProfile())
                .build();
    }

    protected Map<String, String> buildSystemProperties(ResolvedDependency appArtifact, Map<String, String> quarkusProperties) {
        Map<String, String> buildSystemProperties = new HashMap<>();
        buildSystemProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        buildSystemProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        for (Map.Entry<String, String> entry : getForcedProperties().get().entrySet()) {
            if (entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus.")) {
                buildSystemProperties.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : getQuarkusBuildProperties().get().entrySet()) {
            if (entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus.")) {
                buildSystemProperties.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, ?> entry : getProjectProperties().get().entrySet()) {
            if ((entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus."))
                    && entry.getValue() != null) {
                buildSystemProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }

        Set<String> quarkusValues = new HashSet<>();
        quarkusValues.addAll(quarkusProperties.values());
        quarkusValues.addAll(buildSystemProperties.values());

        for (String value : quarkusValues) {
            Expression expression = Expression.compile(value, LENIENT_SYNTAX, NO_TRIM, NO_SMART_BRACES, DOUBLE_COLON);
            for (String reference : expression.getReferencedStrings()) {
                String expanded = getForcedProperties().get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                    continue;
                }

                expanded = getQuarkusBuildProperties().get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                    continue;
                }
                expanded = (String) getProjectProperties().get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                }
            }
        }
        return buildSystemProperties;
    }

    private String getQuarkusProfile() {
        String profile = getQuarkusProfileSystemVariable().getOrNull();
        if (profile == null) {
            profile = getQuarkusProfileEnvVariable().getOrNull();
        }
        if (profile == null) {
            profile = getQuarkusBuildProperties().get().get(QUARKUS_PROFILE);
        }
        if (profile == null) {
            Object p = getQuarkusRelevantProjectProperties().get().get(QUARKUS_PROFILE);
            if (p != null) {
                profile = p.toString();
            }
        }
        if (profile == null) {
            profile = "prod";
        }
        return profile;
    }
}
