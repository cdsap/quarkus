package io.quarkus.gradle.tasks;

import static io.quarkus.runtime.LaunchMode.TEST;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import javax.inject.Inject;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.smallrye.config.SmallRyeConfig;

public abstract class QuarkusPrepareTestTask extends QuarkusTaskWithExtensionView {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    @OutputFile
    public abstract RegularFileProperty getTestProfileKey();

    @Inject
    public QuarkusPrepareTestTask() {
        super("aaaaa", true);
    }

    @TaskAction
    public void generateCode() throws IOException {
        ApplicationModel appModel = ToolingUtils.deserializeAppModel(getApplicationModel().get().getAsFile().toPath());

        SmallRyeConfig config = effectiveProvider().buildEffectiveConfiguration(appModel, new HashMap<>()).getConfig();
        File a = getTestProfileKey().get().getAsFile();
        a.getParentFile().mkdirs();

        String value = config
                .getOptionalValue(TEST.getProfileKey(), String.class)
                .orElse("");

        Files.writeString(a.toPath(), value);

    }
}
