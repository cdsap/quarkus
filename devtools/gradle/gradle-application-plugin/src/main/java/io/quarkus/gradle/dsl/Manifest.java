package io.quarkus.gradle.dsl;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.internal.DefaultAttributes;

public class Manifest implements Serializable {

    private static final long serialVersionUID = 1L;

    private LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    private Map<String, Attributes> sections = new LinkedHashMap<>();

    public Attributes getAttributes() {
        Attributes attributes = new DefaultAttributes();
        attributes.putAll(this.attributes);
        return attributes;
    }

    public Map<String, Attributes> getSections() {
        return sections;
    }

    public Manifest attributes(Map<String, String> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    public Manifest attributes(Map<String, String> attributes, String section) {
        if (!this.sections.containsKey(section)) {
            this.sections.put(section, new DefaultAttributes());
        }
        this.sections.get(section).putAll(attributes);
        return this;
    }

    public void copyTo(Manifest other) {
        other.getAttributes().putAll(attributes);
        other.sections.putAll(sections);
    }
}
