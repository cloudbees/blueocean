package io.jenkins.blueocean.service.embedded.rest;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.model.Run;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.factory.BlueArtifactFactory;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueArtifact;
import org.kohsuke.stapler.Stapler;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ArtifactImpl extends BlueArtifact {
    final private Run run;
    final private Run.Artifact artifact;

    public ArtifactImpl(Run run, Run.Artifact artifact, Link parent) {
        super(parent);
        this.run = run;
        this.artifact = artifact;
    }

    @Override
    public String getName() {
        return artifact.getFileName();
    }

    @Override
    public String getPath() {
        return artifact.relativePath;
    }

    @Override
    public String getUrl() {
        return String.format("/%sartifact/%s", run.getUrl(), artifact.getHref());
    }

    @Override
    public long getSize() {
        try {
            return artifact.getFileSize();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean isDownloadable() {
        return true;
    }

    @Extension(ordinal = -1)
    public static class FactoryImpl extends BlueArtifactFactory {
        @Override
        public Collection<BlueArtifact> getArtifacts(final Run<?, ?> run, final Reachable parent) {
            // TODO: we need to figure out if calling run.getArtifacts() is expensive or not
            return Collections2.transform(run.getArtifactsUpTo(101), new Function<Run.Artifact, BlueArtifact>() {
                @Override
                public BlueArtifact apply(Run.Artifact artifact) {
                    return new ArtifactImpl(run, artifact, parent.getLink());
                }
            });
        }
    }
}
