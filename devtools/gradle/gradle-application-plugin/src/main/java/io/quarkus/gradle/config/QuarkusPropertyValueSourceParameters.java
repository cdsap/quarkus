package io.quarkus.gradle.config;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.ValueSourceParameters;

public interface QuarkusPropertyValueSourceParameters extends ValueSourceParameters {
    ListProperty<String> getPatterns();
}