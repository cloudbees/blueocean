package io.jenkins.blueocean.listeners;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.hal.LinkResolver;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.BuildUpstreamCause;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Listens to creation of jobs that are triggered by an upstream job, much like BuildTriggerListener from
 * pipeline-build-step. Unlike BuildTriggerListener, here we just add an action to the upstream job for later retrieval
 * rather than injecting information into its build log, in order to be a little more flexible at the cost of a bit more
 * work later to retrieve the details.
 */
@Extension
public class DownstreamJobListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(DownstreamJobListener.class.getName());

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        for (Cause cause : run.getCauses()) {
            if (cause instanceof BuildUpstreamCause) {
                BuildUpstreamCause buildUpstreamCause = (BuildUpstreamCause) cause;
                Run triggerRun = buildUpstreamCause.getUpstreamRun();
                if (triggerRun instanceof WorkflowRun) {
                    FlowExecution execution = ((WorkflowRun) triggerRun).getExecution();
                    FlowNode node;

                    if (execution == null) {
                        LOGGER.warning("Could not retrieve upstream FlowExecution");
                        continue;
                    }

                    try {
                        node = execution.getNode(buildUpstreamCause.getNodeId());
                    } catch (IOException e) {
                        LOGGER.warning("Could not retrieve upstream node: " + e);
                        continue;
                    }

                    if (node == null) {
                        LOGGER.warning("Could not retrieve upstream node (null)");
                        continue;
                    }

                    // Add an action on the triggerRun node pointing to the currently executing run
                    String description = run.getDescription();
                    if (description == null) {
                        description = run.getFullDisplayName();
                    }

                    Link link = LinkResolver.resolveLink(run);
                    if (link != null) {
                        try {
                            // Also add to the actual trigger node so we can find it later by step
                            node.addAction(new NodeDownstreamBuildAction(link, description));
                            node.save();

                        } catch (IOException e) {
                            LOGGER.severe("Could not persist node: " + e);
                        }
                    }
                }
            }
        }
    }
}
