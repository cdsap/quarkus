package io.quarkus.gradle.tasks;

import java.util.Collections;
import java.util.function.BiConsumer;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.*;

/**
 * Quarkus task providing inputs compatible with the configuration cache, used by the {@link QuarkusGenerateCode}
 * and {@link QuarkusBuildTask} tasks.
 * <p>
 * Most inputs are provided by the {@link QuarkusPluginExtensionView}. This includes those required by both tasks,
 * and additional inputs that require initialization of the {@link BaseConfig} object.
 * </p>
 * <p>
 * Additionally, this class provides an {@link EffectiveConfigProvider}, which is used by dependent tasks
 * to access the inputs defined in this task.
 * </p>
 */
public abstract class QuarkusTaskWithExtensionView extends QuarkusTask {
    protected static final String QUARKUS_PROFILE = "quarkus.profile";
    private final QuarkusPluginExtensionView extensionView;

    @Input
    @Optional
    public abstract MapProperty<String, Object> getManifestAttributes();

    @Input
    @Optional
    public abstract MapProperty<String, Attributes> getManifestSections();

    @Input
    public abstract ListProperty<String> getPropertiesPattern();

    @Input
    @Optional
    public abstract ListProperty<String> getCachingRelevantProperties();

    @Input
    @Optional
    public abstract MapProperty<String, String> getCachingRelevantProperties2();

    public QuarkusTaskWithExtensionView(String description, boolean compatible) {
        super(description, compatible);
        this.extensionView = getProject().getObjects().newInstance(QuarkusPluginExtensionView.class, extension());
    }

    public BaseConfig baseConfig() {

        EffectiveConfig effectiveConfig = EffectiveConfig.builder()
                .withTaskProperties(Collections.emptyMap())
                .withBuildProperties(getExtensionView().getQuarkusBuildProperties().get())
                .withProjectProperties(getExtensionView().getProjectProperties().get())

                .withSourceDirectories(getExtensionView().getInputFiles().getFiles())
                .withProfile(quarkusProfile())
                .build();
        return new BaseConfig(effectiveConfig);
    }

    public EffectiveConfigProvider effectiveProvider() {
        getManifestAttributes().get().forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String s, Object o) {
                System.out.println("aaaaa" + s + "" + o);
            }
        });
        return new EffectiveConfigProvider(
                getExtensionView().getIgnoredEntries(),
                getExtensionView().getMainResources(),
                getExtensionView().getForcedProperties(),
                getExtensionView().getProjectProperties(),
                getExtensionView().getQuarkusBuildProperties(),
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

    private String quarkusProfile() {
        String profile = System.getProperty(QUARKUS_PROFILE);
        if (profile == null) {
            profile = System.getenv("QUARKUS_PROFILE");
        }
        if (profile == null) {
            profile = getExtensionView().getQuarkusBuildProperties().get().get(QUARKUS_PROFILE);
        }
        if (profile == null) {
            Object p = getExtensionView().getProjectProperties().get().get(QUARKUS_PROFILE);
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
