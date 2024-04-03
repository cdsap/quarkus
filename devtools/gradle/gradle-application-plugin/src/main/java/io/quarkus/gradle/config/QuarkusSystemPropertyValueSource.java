package io.quarkus.gradle.config;

import java.util.Map;

import org.gradle.api.provider.ValueSource;

import io.smallrye.config.common.utils.ConfigSourceUtil;

public abstract class QuarkusSystemPropertyValueSource
        implements ValueSource<Map<String, String>, QuarkusPropertyValueSourceParameters> {

    @Override
    public Map<String, String> obtain() {
        return QuarkusPropertyValueSourceHelper.filter(ConfigSourceUtil.propertiesToMap(System.getProperties()),
                getParameters().getPatterns().get());
    }
}
