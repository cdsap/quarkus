package io.quarkus.gradle.config;

import java.util.Map;

import org.gradle.api.provider.ValueSource;

public abstract class QuarkusEnvVariableValueSource
        implements ValueSource<Map<String, String>, QuarkusPropertyValueSourceParameters> {
    @Override
    public Map<String, String> obtain() {
        return QuarkusPropertyValueSourceHelper.filter(System.getenv(), getParameters().getPatterns().get());
    }
}
