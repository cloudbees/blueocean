/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.blueocean.events;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.hal.LinkResolver;
import io.jenkins.blueocean.service.embedded.rest.OrganizationImpl;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkins.pubsub.EventProps;
import org.jenkins.pubsub.Events;
import org.jenkins.pubsub.JobChannelMessage;
import org.jenkins.pubsub.Message;
import org.jenkins.pubsub.MessageEnricher;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import javax.annotation.Nonnull;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Extension
public class BlueMessageEnricher extends MessageEnricher {

    enum BlueEventProps {
        blueocean_job_rest_url,
        blueocean_job_pipeline_name,
        blueocean_job_branch_name,
    }

    @Override
    public void enrich(@Nonnull Message message) {

        // TODO: Get organization name in generic way once multi-organization support is implemented in API
        message.set(EventProps.Jenkins.jenkins_org, OrganizationImpl.INSTANCE.getName());

        String channelName = message.getChannelName();

        if (!channelName.equals(Events.JobChannel.NAME)) {
            return;
        }

        JobChannelMessage jobChannelMessage = (JobChannelMessage) message;
        ParameterizedJobMixIn.ParameterizedJob job = jobChannelMessage.getJob();
        Link jobUrl = LinkResolver.resolveLink(job);
        if (jobUrl == null) {
            return;
        }

        jobChannelMessage.set(BlueEventProps.blueocean_job_rest_url, jobUrl.getHref());
        jobChannelMessage.set(BlueEventProps.blueocean_job_pipeline_name, job.getFullName());

        ItemGroup<? extends Item> parent = job.getParent();
        if (job instanceof WorkflowJob && parent instanceof WorkflowMultiBranchProject) {
            String multiBranchProjectName = parent.getFullName();
            jobChannelMessage.set(EventProps.Job.job_ismultibranch, "true");
            jobChannelMessage.set(BlueEventProps.blueocean_job_pipeline_name, multiBranchProjectName);
            jobChannelMessage.set(BlueEventProps.blueocean_job_branch_name, job.getName());
        }
    }
}
