package io.quarkus.gradle.config;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QuarkusPropertyValueSourceHelper {
    public static Map<String, String> filter(Map<String, String> properties, List<String> rawPatterns) {
        List<Pattern> patterns = rawPatterns.stream()
                .map(s -> "^(" + s + ")$")
                .map(Pattern::compile)
                .collect(Collectors.toList());
        Predicate<Map.Entry<String, ?>> keyPredicate = e -> patterns.stream().anyMatch(p -> p.matcher(e.getKey()).matches());
        return properties.entrySet().stream()
                .filter(keyPredicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
