package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;

import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.config.QuarkusPropertyValueSourceHelper;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.runtime.configuration.ConfigInstantiator;

/**
 * Required parts of the configuration used to <em>configure</em> a Quarkus build task, does not contain settings
 * via the {@link io.quarkus.gradle.extension.QuarkusPluginExtension} or any "forced properties".
 *
 * <p>
 * Configuration from system properties, environment, application.properties/yaml/yml, project properties is
 * available in a Gradle task's configuration phase.
 */
public final class BaseConfig {
    private final Manifest manifest;
    private final PackageConfig packageConfig;
    private final Map<String, String> configMap;

    // Note: EffectiveConfig has all the code to load the configurations from all the sources.
    BaseConfig(EffectiveConfig config, Manifest userDefinedManifest) {
        manifest = new Manifest();
        packageConfig = new PackageConfig();

        ConfigInstantiator.handleObject(packageConfig, config.config());

        // populate the Gradle Manifest object
        manifest.attributes(packageConfig.manifest.attributes);
        packageConfig.manifest.manifestSections.forEach((section, attribs) -> manifest.attributes(attribs, section));
        userDefinedManifest.copyTo(manifest);

        configMap = config.configMap();
    }

    public PackageConfig packageConfig() {
        return packageConfig;
    }

    PackageConfig.BuiltInType packageType() {
        return PackageConfig.BuiltInType.fromString(packageConfig.type);
    }

    Manifest manifest() {
        return manifest;
    }

    Map<String, String> cachingRelevantProperties(List<String> propertyPatterns) {
        return QuarkusPropertyValueSourceHelper.filter(configMap, propertyPatterns);
    }
}
