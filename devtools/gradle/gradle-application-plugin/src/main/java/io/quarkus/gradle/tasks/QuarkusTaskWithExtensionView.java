package io.quarkus.gradle.tasks;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import io.quarkus.gradle.dsl.Manifest;

/**
 * Qyarkus tasks holding {@link QuarkusPluginExtensionView} for configuration.
 * Tasks compatible with Configuration cache need to provide the task inputs via the extension view.
 * Currently, QuarkusBuildTask and QuarkusGenerateCodeTask are the tasks exending this class.
 * This class also provides an EffectiveConfigProvider to access the effective configuration using the extension view
 * inputs and other inputs thar require instantiating `baseConfig`
 *
 * <p>
 * Configuration from system properties, environment, application.properties/yaml/yml, project properties is
 * available in a Gradle task's configuration phase.
 */
public abstract class QuarkusTaskWithExtensionView extends QuarkusTask {
    private final QuarkusPluginExtensionView extensionView;

    @Input
    @Optional
    public abstract MapProperty<String, Object> getManifestAttributes();

    @Input
    @Optional
    public abstract MapProperty<String, Attributes> getManifestSections();

    @Input
    @Optional
    public abstract Property<Manifest> getManifest();

    @Input
    public abstract MapProperty<String, String> getCachingRelevantInput();

    public QuarkusTaskWithExtensionView(String description, boolean compatible) {
        super(description, compatible);
        this.extensionView = getProject().getObjects().newInstance(QuarkusPluginExtensionView.class, extension());
    }

    public EffectiveConfigProvider effectiveProvider() {
        return new EffectiveConfigProvider(
                getExtensionView().getIgnoredEntries(),
                getExtensionView().getMainResources(),
                getExtensionView().getForcedProperties(),
                getExtensionView().getProjectProperties(),
                getExtensionView().getQuarkusBuildProperties(),
                getExtensionView().getQuarkusRelevantProjectProperties(),
                getManifestAttributes(),
                getManifestSections(),
                getExtensionView().getNativeBuild(),
                getExtensionView().getQuarkusProfileSystemVariable(),
                getExtensionView().getQuarkusProfileEnvVariable());
    }

    /**
     * Returns a view of the Quarkus extension that is compatible with the configuration cache.
     */
    @Nested
    protected QuarkusPluginExtensionView getExtensionView() {
        return extensionView;
    }
}
