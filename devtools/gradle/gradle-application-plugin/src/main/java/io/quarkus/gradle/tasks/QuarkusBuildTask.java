package io.quarkus.gradle.tasks;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.StopExecutionException;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.maven.dependency.GACTV;

/**
 * Base class for the {@link QuarkusBuildDependencies}, {@link QuarkusBuildCacheableAppParts}, {@link QuarkusBuild} tasks
 */
abstract class QuarkusBuildTask extends QuarkusTask {

    private final transient QuarkusPluginExtension extension;

    private final GACTV gactv;

    QuarkusBuildTask(String description) {
        super(description);
        this.extension = getProject().getExtensions().findByType(QuarkusPluginExtension.class);
        gactv = new GACTV(getProject().getGroup().toString(), getProject().getName(),
                getProject().getVersion().toString());
        // Calling this method tells Gradle that it should not fail the build. Side effect is that the configuration
        // cache will be at least degraded, but the build will not fail.
        notCompatibleWithConfigurationCache("The Quarkus Plugin isn't compatible with the configuration cache");
    }

    QuarkusPluginExtension extension() {
        return extension;
    }

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Classpath
    public FileCollection getClasspath() {
        return extension().classpath();
    }

    @Input
    public Map<String, String> getCachingRelevantInput() {
        ListProperty<String> vars = extension().getCachingRelevantProperties();
        return extension().baseConfig().cachingRelevantProperties(vars.get());
    }

    ApplicationModel resolveAppModelForBuild() {
        ApplicationModel appModel;
        try {
            appModel = extension().getAppModelResolver().resolveModel(gactv);
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve Quarkus application model for " + getPath(), e);
        }
        return appModel;
    }

    void abort(String message, Object... args) {
        getLogger().warn(message, args);
        getProject().getTasks().stream()
                .filter(t -> t != this)
                .filter(t -> !t.getState().getExecuted()).forEach(t -> {
                    t.setEnabled(false);
                });
        throw new StopExecutionException();
    }
}
