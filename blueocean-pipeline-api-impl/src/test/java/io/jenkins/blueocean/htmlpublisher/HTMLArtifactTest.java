package io.jenkins.blueocean.htmlpublisher;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterators;
import com.google.common.io.Resources;
import hudson.model.Run;
import io.jenkins.blueocean.rest.factory.BluePipelineFactory;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineBaseTest;
import io.jenkins.blueocean.rest.model.BlueArtifact;
import io.jenkins.blueocean.rest.model.BlueArtifactContainer;
import io.jenkins.blueocean.rest.model.BluePipeline;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class HTMLArtifactTest extends PipelineBaseTest {

    @Test
    public void resolveArtifact() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "project");

        URL resource = Resources.getResource(getClass(), "HTMLArtifactTest.jenkinsfile");
        String jenkinsFile = Resources.toString(resource, Charsets.UTF_8);
        p.setDefinition(new CpsFlowDefinition(jenkinsFile, true));
        p.save();

        Run r = p.scheduleBuild2(0).waitForStart();
        j.waitForCompletion(r);

        BluePipeline  bluePipeline = (BluePipeline) BluePipelineFactory.resolve(p);
        BlueArtifactContainer artifacts = bluePipeline.getLatestRun().getArtifacts();

        Assert.assertEquals(1, Iterators.size(artifacts.iterator()));

        BlueArtifact artifact = Iterators.getOnlyElement(artifacts.iterator());
        Assert.assertEquals("/blue/rest/organizations/jenkins/pipelines/project/runs/1/artifacts/io.jenkins.blueocean.htmlpublisher.HTMLArtifact%253AMy%252520Cool%252520report/", artifact.getLink().getHref());
        Assert.assertEquals("My Cool report", artifact.getName());
        Assert.assertEquals("My Cool report", artifact.getPath());
        Assert.assertNotNull(artifact.getUrl());
        Assert.assertEquals(-1, artifact.getSize());
        Assert.assertFalse(artifact.isDownloadable());
    }
}
