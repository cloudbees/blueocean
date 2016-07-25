package io.jenkins.blueocean.rest.impl.pipeline;

import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueActionProxy;
import io.jenkins.blueocean.rest.model.BluePipelineNode;
import io.jenkins.blueocean.rest.model.BluePipelineStep;
import io.jenkins.blueocean.rest.model.BluePipelineStepContainer;
import io.jenkins.blueocean.rest.model.BlueRun;
import io.jenkins.blueocean.service.embedded.rest.PipelineImpl;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Implementation of {@link BluePipelineNode}.
 *
 * @author Vivek Pandey
 * @see FlowNode
 */
public class PipelineNodeImpl extends BluePipelineNode {
    private final FlowNode node;
    private final List<FlowNode> children;
    private final List<Edge> edges;
    private final WorkflowRun run;
    private final Long durationInMillis;
    private final PipelineNodeGraphBuilder.NodeRunStatus status;
    private final PipelineNodeGraphBuilder nodeGraphBuilder;
    private final Link self;

    public PipelineNodeImpl(WorkflowRun run, final FlowNode node, PipelineNodeGraphBuilder.NodeRunStatus status, PipelineNodeGraphBuilder nodeGraphBuilder, Link parentLink) {
        this.run = run;
        this.node = node;
        this.children = nodeGraphBuilder.getChildren(node);
        this.edges = buildEdges();
        this.status = status;
        if(getStateObj() == BlueRun.BlueRunState.FINISHED){
            this.durationInMillis = nodeGraphBuilder.getDurationInMillis(node);
        }else if(getStateObj() == BlueRun.BlueRunState.RUNNING){
            this.durationInMillis = System.currentTimeMillis()-TimingAction.getStartTime(node);
        }else{
            this.durationInMillis = null;
        }
        this.nodeGraphBuilder = nodeGraphBuilder;
        this.self = parentLink.rel(node.getId());
    }

    @Override
    public String getId() {
        return node.getId();
    }

    @Override
    public String getDisplayName() {
        return PipelineNodeUtil.getDisplayName(node);
    }

    @Override
    public BlueRun.BlueRunResult getResult() {
        if(isInactiveNode()){
            return null;
        }
        return status.getResult();
    }

    @Override
    public BlueRun.BlueRunState getStateObj() {
        if(isInactiveNode()){
            return null;
        }
        return status.getState();
    }

    @Override
    public Date getStartTime() {
        if(isInactiveNode()){
            return null;
        }
        long nodeTime = TimingAction.getStartTime(node);
        return new Date(nodeTime);
    }

    @Override
    public List<Edge> getEdges() {
        return edges;
    }

    @Override
    public Long getDurationInMillis() {
        return durationInMillis;
    }

    /**
     * No logs for Node as Node by itself doesn't have any log to repot, its steps inside it that has logs
     *
     * @see BluePipelineStep#getLog()
     */
    @Override
    public Object getLog() {
        return null;
    }

    @Override
    public BluePipelineStepContainer getSteps() {
        return new PipelineStepContainerImpl(node, nodeGraphBuilder, self);
    }

    @Override
    public Link getLink() {
        return self;
    }

    @Override
    public Collection<BlueActionProxy> getActions() {
        return PipelineImpl.getActionProxies(node.getAllActions(), this);
    }


    public static class EdgeImpl extends Edge{
        private final FlowNode node;
        private final FlowNode edge;

        public EdgeImpl(FlowNode node, FlowNode edge) {
            this.node = node;
            this.edge = edge;
        }

        @Override
        public String getId() {
            return edge.getId();
        }
    }

    private List<Edge> buildEdges(){
        List<Edge> edges  = new ArrayList<>();
        if(!this.children.isEmpty()) {
            for (final FlowNode c : children) {
                edges.add(new EdgeImpl(node, c));
            }
        }
        return edges;
    }

    private boolean isInactiveNode(){
        return node instanceof PipelineNodeGraphBuilder.InactiveFlowNodeWrapper;
    }
}
