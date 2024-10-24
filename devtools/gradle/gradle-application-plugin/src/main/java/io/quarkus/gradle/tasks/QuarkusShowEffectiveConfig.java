package io.quarkus.gradle.tasks;

import static java.lang.String.format;
import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.gradle.QuarkusPlugin;
import io.smallrye.config.SmallRyeConfig;

/**
 * Just show the effective configuration and settings.
 */
public abstract class QuarkusShowEffectiveConfig extends QuarkusBuildTaskWithConfigurationCache {

    private final Property<Boolean> saveConfigProperties;

    @Inject
    public QuarkusShowEffectiveConfig() {
        super("Collect dependencies for the Quarkus application, prefer the 'quarkusBuild' task");
        this.saveConfigProperties = getProject().getObjects().property(Boolean.class).convention(Boolean.FALSE);
    }

    @Option(option = "save-config-properties", description = "Save the effective Quarkus configuration properties to a file.")
    @Internal
    public Property<Boolean> getSaveConfigProperties() {
        return saveConfigProperties;
    }

    @TaskAction
    public void dumpEffectiveConfiguration() {
        try {
            SmallRyeConfig config = getSmallRyeConfig().get();
            List<String> sourceNames = new ArrayList<>();
            config.getConfigSources().forEach(configSource -> sourceNames.add(configSource.getName()));
            String quarkusConfig = getSmallRyeConfigValues().get()
                    .entrySet()
                    .stream()
                    .map(e -> format("quarkus.%s=%s", e.getKey(), e.getValue())).sorted()
                    .collect(Collectors.joining("\n    ", "\n    ", "\n"));
            getLogger().lifecycle("Effective Quarkus configuration options: {}", quarkusConfig);

            String finalName = getFinalName().get();
            String packageType = config.getOptionalValue(QuarkusPlugin.QUARKUS_PACKAGE_TYPE, String.class).orElse("fast-jar");
            File fastJar = fastJar();
            getLogger().lifecycle(
                    "Quarkus package type:          {}\n" +
                            "Final name:                    {}\n" +
                            "Output directory:              {}\n" +
                            "Fast jar directory (if built): {}\n" +
                            "Runner jar (if built):         {}\n" +
                            "Native runner (if built):      {}\n" +
                            "application.(properties|yaml|yml) sources: {}",
                    packageType,
                    finalName,
                    getOutputDirectory().get(),
                    fastJar,
                    runnerJar(),
                    nativeRunner(),
                    sourceNames.stream().collect(Collectors.joining("\n        ", "\n        ", "\n")));

            if (getSaveConfigProperties().get()) {
                Properties props = new Properties();
                props.putAll(getEffectiveConfigProperties().get());
                Path file = buildDir.toPath().resolve(finalName + ".quarkus-build.properties");
                try (BufferedWriter writer = newBufferedWriter(file)) {
                    props.store(writer, format("Quarkus build properties with package type %s", packageType));
                } catch (IOException e) {
                    throw new GradleException("Failed to write Quarkus build configuration settings", e);
                }
                getLogger().lifecycle("\nWrote configuration settings to {}", file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GradleException("WTF", e);
        }
    }
}
