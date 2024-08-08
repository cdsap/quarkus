package io.quarkus.gradle.tasks;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.google.common.annotations.VisibleForTesting;

import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.configuration.ConfigCompatibility;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.smallrye.config.*;
import io.smallrye.config.common.utils.ConfigSourceUtil;
import io.smallrye.config.source.yaml.YamlConfigSourceLoader;

/**
 * Helper that bundles the various sources of config options for the Gradle plugin: system environment, system properties,
 * quarkus build properties (on the Quarkus extension), Gradle project properties, application properties and "forced/task"
 * properties (on the Gradle task).
 *
 * <p>
 * Eventually used to construct a map with the <em>effective</em> config options from all the sources above and expose
 * the Quarkus config objects like {@link PackageConfig}, {@link ClassLoadingConfig} and the underlying {@link SmallRyeConfig}.
 */
public final class EffectiveConfig {
    private final SmallRyeConfig config;
    private final Map<String, String> values;

    private EffectiveConfig(Builder builder) {
        List<ConfigSource> configSources = new ArrayList<>();
        configSources.add(new PropertiesConfigSource(builder.forcedProperties, "forcedProperties", 600));
        configSources.add(new PropertiesConfigSource(asStringMap(builder.taskProperties), "taskProperties", 500));
        Map<String, String> systemPropertiesWithoutConfCacheProblematicEntries = ConfigSourceUtil
                .propertiesToMap(System.getProperties());
        systemPropertiesWithoutConfCacheProblematicEntries.remove("idea.io.use.nio2");
        configSources.add(new PropertiesConfigSource(systemPropertiesWithoutConfCacheProblematicEntries,
                "System.getProperties()", 400));
        //  Commented the EnvConfigSource because configuration cache is invalidated on every build because:
        //  "Calculating task graph as configuration cache cannot be reused because environment variable 'APP_ICON_65671' has changed."
        //        configSources.add(new EnvConfigSource(300) {
        //        });
        //        })
        configSources.add(new PropertiesConfigSource(builder.buildProperties, "quarkusBuildProperties", 290));
        configSources.add(new PropertiesConfigSource(asStringMap(builder.projectProperties), "projectProperties", 280));

        // todo: this is due to ApplicationModel#getPlatformProperties not being included in the effective config
        configSources.add(new PropertiesConfigSource(Map.of("platform.quarkus.native.builder-image", "<<ignored>>"),
                "NativeConfig#builderImage", 0));
        // Effective "ordinals" for the config sources:
        // (see also https://quarkus.io/guides/config-reference#configuration-sources)
        // 600 -> forcedProperties
        // 500 -> taskProperties
        // 400 -> System.getProperties() (provided by default sources)
        // 300 -> System.getenv() (provided by default sources)
        // 290 -> quarkusBuildProperties
        // 280 -> projectProperties
        // 265 -> application.(yaml/yml) in config folder
        // 260 -> application.properties in config folder (provided by default sources)
        // 255 -> application.(yaml|yml) in classpath
        // 250 -> application.properties in classpath (provided by default sources)
        // 110 -> microprofile.(yaml|yml) in classpath
        // 100 -> microprofile.properties in classpath (provided by default sources)
        // 0 -> fallback config source for error workaround (see below)

        // Removing problematic property breaking configuration cache
        Map<String, String> defaultPropertiesFiltered = builder.defaultProperties;
        defaultPropertiesFiltered.remove("idea.io.use.nio2");
        SmallRyeConfigBuilder builder1 = new SmallRyeConfigBuilder()
                .forClassLoader(Thread.currentThread().getContextClassLoader())
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
                .addDiscoveredConverters()
                .addDefaultInterceptors()
                .addDiscoveredInterceptors()
                //    .addPropertiesSources()
                .addDiscoveredSecretKeysHandlers();

        this.config = builder1
                .forClassLoader(toUrlClassloader(builder.sourceDirectories))
                .withSources(configSources)

                .withSources(new YamlConfigSourceLoader.InFileSystem())
                .withSources(new YamlConfigSourceLoader.InClassPath())
                //    .addPropertiesSources()
                // todo: this is due to ApplicationModel#getPlatformProperties not being included in the effective config
                .withSources(new PropertiesConfigSource(Map.of("platform.quarkus.native.builder-image", "<<ignored>>"),
                        "NativeConfig#builderImage", 0))

                //     .withDefaultValues(defaultPropertiesFiltered)
                .withProfile(builder.profile)
                .withMapping(PackageConfig.class)
                .withMapping(NativeConfig.class)
                .withInterceptors(ConfigCompatibility.FrontEnd.instance(), ConfigCompatibility.BackEnd.instance())
                .setAddDiscoveredSecretKeysHandlers(false)
                .build();
        this.values = generateFullConfigMap(config);
    }

    public SmallRyeConfig getConfig() {
        return config;
    }

    public Map<String, String> getValues() {
        return values;
    }

    private Map<String, String> asStringMap(Map<String, ?> map) {
        Map<String, String> target = new HashMap<>();
        map.forEach((k, v) -> {
            if (v != null) {
                target.put(k, v.toString());
            }
        });
        return target;
    }

    @VisibleForTesting
    static Map<String, String> generateFullConfigMap(SmallRyeConfig config) {
        return Expressions.withoutExpansion(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : config.getPropertyNames()) {
                    String value = config.getRawValue(propertyName);
                    if (value != null) {
                        properties.put(propertyName, value);
                    }
                }
                return unmodifiableMap(properties);
            }
        });
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private Map<String, String> forcedProperties = emptyMap();
        private Map<String, ?> taskProperties = emptyMap();
        private Map<String, String> buildProperties = emptyMap();
        private Map<String, ?> projectProperties = emptyMap();
        private Map<String, String> defaultProperties = emptyMap();
        private Set<File> sourceDirectories = emptySet();
        private String profile = "prod";

        EffectiveConfig build() {
            return new EffectiveConfig(this);
        }

        Builder withForcedProperties(Map<String, String> forcedProperties) {
            this.forcedProperties = forcedProperties;
            return this;
        }

        Builder withTaskProperties(Map<String, ?> taskProperties) {
            this.taskProperties = taskProperties;
            return this;
        }

        Builder withBuildProperties(Map<String, String> buildProperties) {
            this.buildProperties = buildProperties;
            return this;
        }

        Builder withProjectProperties(Map<String, ?> projectProperties) {
            this.projectProperties = projectProperties;
            return this;
        }

        Builder withDefaultProperties(Map<String, String> defaultProperties) {
            // this.defaultProperties = defaultProperties;
            return this;
        }

        Builder withSourceDirectories(Set<File> sourceDirectories) {
            this.sourceDirectories = sourceDirectories;
            return this;
        }

        Builder withProfile(String profile) {
            this.profile = profile;
            return this;
        }
    }

    private static ClassLoader toUrlClassloader(Set<File> sourceDirectories) {
        List<URL> urls = new ArrayList<>();
        for (File sourceDirectory : sourceDirectories) {
            try {
                urls.add(sourceDirectory.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]));
    }
}
