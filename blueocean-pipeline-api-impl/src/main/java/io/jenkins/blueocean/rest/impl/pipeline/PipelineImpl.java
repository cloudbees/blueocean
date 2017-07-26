package io.jenkins.blueocean.rest.impl.pipeline;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.annotation.Capability;
import io.jenkins.blueocean.rest.factory.BluePipelineFactory;
import io.jenkins.blueocean.rest.factory.organization.OrganizationFactory;
import io.jenkins.blueocean.rest.model.BlueOrganization;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.Resource;
import io.jenkins.blueocean.service.embedded.rest.AbstractPipelineImpl;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import static io.jenkins.blueocean.rest.model.KnownCapabilities.JENKINS_WORKFLOW_JOB;

/**
 * @author Kohsuke Kawaguchi
 */
@Capability({JENKINS_WORKFLOW_JOB})
public class PipelineImpl extends AbstractPipelineImpl {
    protected PipelineImpl(BlueOrganization organization, Job job) {
        super(organization, job);
    }

    @Extension(ordinal = 1)
    public static class PipelineFactoryImpl extends BluePipelineFactory {

        @Override
        public BluePipeline getPipeline(Item item, Reachable parent) {
            BlueOrganization org = OrganizationFactory.getInstance().getContainingOrg(item);
            if (org != null && item instanceof WorkflowJob) {
                return new PipelineImpl(org, (Job) item);
            }
            return null;
        }

        @Override
        public Resource resolve(Item context, Reachable parent, Item target) {
            if(context == target && target instanceof WorkflowJob) {
                return getPipeline(target,parent);
            }

            return null;
        }
    }
}
