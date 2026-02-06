package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.QuarkusBuildPropertiesResolver.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.java.archives.Attributes;

import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.extension.QuarkusPluginExtension;

public class QuarkusPackageValues {

    private final Boolean experimental;
    private final QuarkusPluginExtension extension;

    private final Map<String, String> properties;

    private final Project project;

    public QuarkusPackageValues(Boolean experimental,
            Project project,
            QuarkusPluginExtension extension) {
        this.experimental = experimental;
        this.extension = extension;
        if (experimental) {
            properties = QuarkusBuildPropertiesResolver.resolve(project, extension);
        } else {
            properties = Map.of();
        }
        this.project = project;
    }

    public Boolean getJarEnabled() {
        if (experimental) {
            return Boolean.parseBoolean(properties.get(KEY_JAR_ENABLED));
        } else {
            return extension.packageConfig().jar().enabled();
        }
    }

    public Boolean getNativeEnabled() {
        if (experimental) {
            return Boolean.parseBoolean(properties.get(KEY_NATIVE_ENABLED));
        } else {
            return extension.nativeConfig().enabled();
        }
    }

    public Path getOutputDirectory() {
        if (experimental) {
            return Path.of(properties.get(KEY_OUTPUT_DIRECTORY));
        } else {
            return Path.of(extension.packageConfig().outputDirectory().map(Path::toString)
                    .orElse(QuarkusPlugin.DEFAULT_OUTPUT_DIRECTORY));
        }
    }

    public String getOutputName() {
        if (experimental) {
            return properties.get(KEY_OUTPUT_NAME);
        } else {
            return extension.packageConfig().outputName().orElseGet(extension::finalName);
        }
    }

    public Boolean getNativeSourcesOnly() {
        if (experimental) {
            return Boolean.parseBoolean(properties.get(KEY_NATIVE_SOURCES_ONLY));
        } else {
            return extension.nativeConfig().sourcesOnly();
        }
    }

    public PackageConfig.JarConfig.JarType getJarType() {
        if (experimental) {
            return PackageConfig.JarConfig.JarType.fromString(properties.get(KEY_JAR_TYPE));
        } else {
            return extension.packageConfig().jar().type();
        }
    }

    public String getRunnerSuffix() {
        if (experimental) {
            if (Boolean.parseBoolean(properties.get(KEY_ADD_RUNNER_SUFFIX))) {
                return properties.get(KEY_RUNNER_SUFFIX);
            } else {
                return "";
            }
        } else {
            return extension.packageConfig().computedRunnerSuffix();
        }
    }

    public Map<String, String> getCachingRelevantProperties(List<String> cachingRelevantProperties) {
        if (experimental) {
            return cachingRelevantProperties.stream()
                    .filter(s -> !"quarkus[.].*".equals(s) && !"platform[.]quarkus[.].*".equals(s))
                    .map(s -> Map.entry(s, project.getProviders().environmentVariable(s).getOrNull()))
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        } else {
            return extension.cachingRelevantProperties(extension.getCachingRelevantProperties().get());
        }

    }

    public Map<String, Object> getManifestAttributes() {
        if (experimental) {
            return Map.of();
        } else {
            return extension.manifest().getAttributes();
        }
    }

    public Map<String, Attributes> getManifestSections() {
        if (experimental) {
            return Map.of();
        } else {
            return extension.manifest().getSections();
        }
    }

    public List<String> getIgnoredEntries() {
        if (experimental) {
            String userIgnoredEntries = properties.get(KEY_IGNORED_ENTRIES);
            System.out.println("11111");
            if (userIgnoredEntries != null && !userIgnoredEntries.isEmpty()) {
                return List.of(userIgnoredEntries.split(","));
            } else {
                return List.of();
            }
        } else {
            return extension.ignoredEntriesProperty().get();
        }
    }
}
