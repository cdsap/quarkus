package io.quarkus.bootstrap.workspace;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class DefaultArtifactSources implements ArtifactSources, Serializable {

    private static final long serialVersionUID = 2053702489268820757L;

    private final String classifier;
    private final List<SourceDir> sources;
    private final List<SourceDir> resources;

    public DefaultArtifactSources(String classifier, List<SourceDir> sources, List<SourceDir> resources) {
        this.classifier = Objects.requireNonNull(classifier, "The classifier is null");
        this.sources = sources;
        this.resources = resources;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    public void addSources(SourceDir src) {
        this.sources.add(src);
    }

    @Override
    public List<SourceDir> getSourceDirs() {
        return sources;
    }

    public void addResources(SourceDir src) {
        this.resources.add(src);
    }

    @Override
    public List<SourceDir> getResourceDirs() {
        return resources;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(classifier);
        if (s.length() > 0) {
            s.append(' ');
        }
        s.append("sources: ").append(sources);
        s.append(" resources: ").append(resources);
        return s.toString();
    }
}
