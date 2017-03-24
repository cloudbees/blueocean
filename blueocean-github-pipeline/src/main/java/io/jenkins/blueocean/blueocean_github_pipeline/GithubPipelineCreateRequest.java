package io.jenkins.blueocean.blueocean_github_pipeline;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.pubsub.MessageException;
import org.jenkinsci.plugins.pubsub.PubsubBus;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;

import hudson.model.Cause;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.model.User;
import io.jenkins.blueocean.commons.ErrorMessage;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.impl.pipeline.credential.BlueOceanCredentialsProvider;
import io.jenkins.blueocean.rest.impl.pipeline.credential.BlueOceanDomainRequirement;
import io.jenkins.blueocean.rest.impl.pipeline.credential.CredentialsUtils;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.BlueScmConfig;
import io.jenkins.blueocean.service.embedded.rest.AbstractPipelineCreateRequestImpl;
import jenkins.branch.CustomOrganizationFolderDescriptor;
import jenkins.branch.MultiBranchProject;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMProbeStat;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceEvent;

/**
 * @author Vivek Pandey
 */
public class GithubPipelineCreateRequest extends AbstractPipelineCreateRequestImpl {

    private static final String DESCRIPTOR = "jenkins.branch.OrganizationFolder.org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator";
    private static final Logger logger = LoggerFactory.getLogger(GithubPipelineCreateRequest.class);

    private BlueScmConfig scmConfig;

    @DataBoundConstructor
    public GithubPipelineCreateRequest(String name, BlueScmConfig scmConfig) {
        setName(name);
        this.scmConfig = scmConfig;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BluePipeline create(Reachable parent) throws IOException {

        String apiUrl = null;
        String orgName = getName(); //default
        String credentialId = null;
        List<String> repos = new ArrayList<>();

        if (scmConfig != null) {
            apiUrl = StringUtils.defaultIfBlank(scmConfig.getUri(), GithubScm.DEFAULT_API_URI);
            if (scmConfig.getConfig().get("orgName") instanceof String) {
                orgName = (String) scmConfig.getConfig().get("orgName");
            }
            credentialId = scmConfig.getCredentialId();
            if (scmConfig != null && scmConfig.getConfig().get("repos") instanceof List) {
                for (String r : (List<String>) scmConfig.getConfig().get("repos")) {
                    repos.add(r);
                }
            }
        }

        String singleRepo = repos.size() == 1 ? repos.get(0) : null;

        User authenticatedUser =  User.current();
        if(authenticatedUser == null){
            throw new ServiceException.UnauthorizedException("Must login to create a pipeline");
        }

        Item item = Jenkins.getInstance().getItemByFullName(orgName);
        try {
            if(credentialId != null) {
                validateCredentialId(credentialId, apiUrl);
            }

            if (item == null) {
                item = create(Jenkins.getInstance(), getName(), DESCRIPTOR, CustomOrganizationFolderDescriptor.class);
            }

            if (item instanceof OrganizationFolder) {
                if(credentialId != null) {
                    //Find domain attached to this credentialId, if present check if it's BlueOcean specific domain then
                    //add the properties otherwise simply use it
                    Domain domain = CredentialsUtils.findDomain(credentialId, authenticatedUser);
                    if(domain == null){ //this should not happen since validateCredentialId found the credential
                        throw new ServiceException.BadRequestExpception(
                                new ErrorMessage(400, "Failed to create pipeline")
                                        .add(new ErrorMessage.Error("scm.credentialId",
                                                ErrorMessage.Error.ErrorCodes.INVALID.toString(),
                                                "No domain in user credentials found for credentialId: "+ scmConfig.getCredentialId())));
                    }
                    if(domain.test(new BlueOceanDomainRequirement())) {
                        ((OrganizationFolder) item)
                                .addProperty(
                                        new BlueOceanCredentialsProvider.FolderPropertyImpl(
                                                authenticatedUser.getId(), credentialId,
                                                BlueOceanCredentialsProvider.createDomain(apiUrl)
                                        ));
                    }
                }

                // cick of github scan build
                OrganizationFolder organizationFolder = (OrganizationFolder) item;

                GitHubSCMNavigator gitHubSCMNavigator = organizationFolder.getNavigators().get(GitHubSCMNavigator.class);

                StringBuilder sb = new StringBuilder();
                if (gitHubSCMNavigator != null) {
                    // currently, we are setting a series of regular expressions to match the repositories
                    // so we need to extract the current set for incoming create requests to keep them
                    // see a few lines below for the pattern being used
                    Matcher matcher = Pattern.compile("\\((.*?)\\\\b\\)\\?").matcher(gitHubSCMNavigator.getPattern());

                    while (matcher.find()) {
                        String existingRepo = matcher.group(1);
                        if (!repos.contains(existingRepo)) {
                            repos.add(existingRepo);
                        }
                    }

                    if (credentialId == null) {
                        credentialId = gitHubSCMNavigator.getScanCredentialsId();
                    }
                }

                gitHubSCMNavigator = new GitHubSCMNavigator(apiUrl, orgName, credentialId, credentialId);
                organizationFolder.getNavigators().replace(gitHubSCMNavigator);

                for (String r : repos) {
                    sb.append(String.format("(%s\\b)?", r));
                }
                if (sb.length() > 0) {
                    gitHubSCMNavigator.setPattern(sb.toString());
                }

                GithubOrganizationFolder githubOrganizationFolder = new GithubOrganizationFolder(organizationFolder, parent.getLink());
                if(singleRepo != null){
                    final boolean hasJenkinsfile = repoHasJenkinsFile(apiUrl,credentialId, orgName, singleRepo, organizationFolder);
                    if(hasJenkinsfile){
                        SCMSourceEvent.fireNow(new SCMSourceEventImpl(singleRepo, item, apiUrl, gitHubSCMNavigator));
                        sendMultibranchIndexingCompleteEvent(item, organizationFolder, singleRepo, 5);
                    } else {
                        sendOrganizationScanCompleteEvent(item, organizationFolder);
                    }
                    githubOrganizationFolder.addRepo(singleRepo, new GithubOrganizationFolder.BlueRepositoryProperty(){
                        @Override
                        public boolean meetsIndexingCriteria() {
                            return hasJenkinsfile;
                        }
                    });
                }else {
                    organizationFolder.scheduleBuild(new Cause.UserIdCause());
                }
                return githubOrganizationFolder;
            }
        } catch (Exception e){
            String msg = String.format("Error creating pipeline %s: %s",getName(),e.getMessage());
            logger.error(msg, e);
            if(item != null) {
                try {
                    item.delete();
                } catch (InterruptedException e1) {
                    logger.error(String.format("Error creating pipeline %s: %s",getName(),e1.getMessage()), e1);
                    throw new ServiceException.UnexpectedErrorException("Error cleaning up pipeline " + getName() + " due to error: " + e.getMessage(), e);
                }
            }
            if(e instanceof ServiceException){
                throw (ServiceException)e;
            }
            throw new ServiceException.UnexpectedErrorException(msg, e);
        }
        return null;
    }

    private void sendOrganizationScanCompleteEvent(final Item item, final OrganizationFolder orgFolder) {
        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                _sendOrganizationScanCompleteEvent(item, orgFolder);
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void _sendOrganizationScanCompleteEvent(Item item, OrganizationFolder orgFolder) {
        try {
            org.jenkinsci.plugins.pubsub.SimpleMessage msg = new org.jenkinsci.plugins.pubsub.SimpleMessage();
            msg.set("jenkins_object_type","jenkins.branch.OrganizationFolder");
            msg.set("job_run_status","ALLOCATED");
            msg.set("job_name",orgFolder.getName());
            msg.set("jenkins_org","jenkins");
            msg.set("job_orgfolder_indexing_status","COMPLETE");
            msg.set("job_run_queueId","1");
            msg.set("jenkins_object_name",orgFolder.getName());
            msg.set("blueocean_job_rest_url","/blue/rest/organizations/jenkins/pipelines/"+orgFolder.getName()+"/");
            msg.set("jenkins_event","job_run_queue_task_complete");
            msg.set("job_orgfolder_indexing_result","SUCCESS");
            msg.set("blueocean_job_pipeline_name",orgFolder.getName());
            msg.set("jenkins_object_url","job/"+orgFolder.getName()+"/");
            msg.set("jenkins_channel","job");
            PubsubBus.getBus().publish(msg);
        } catch (MessageException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMultibranchIndexingCompleteEvent(final Item item, final OrganizationFolder orgFolder, final String name, final int iterations) {
        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                _sendMultibranchIndexingCompleteEvent(item, orgFolder, name, iterations);
            }
        }, 1, TimeUnit.SECONDS);
    }

    @SuppressWarnings({ "rawtypes" })
    private void _sendMultibranchIndexingCompleteEvent(Item item, OrganizationFolder orgFolder, String name, int iterations) {
        MultiBranchProject mbp = orgFolder.getItem(name);
        if (mbp == null) {
            if (iterations <= 0) {
                return; // not found
            }
            sendMultibranchIndexingCompleteEvent(item, orgFolder, name, iterations - 1);
            return;
        }
        try {
            String jobName = orgFolder.getName() + "/" + mbp.getName();
            org.jenkinsci.plugins.pubsub.SimpleMessage msg = new org.jenkinsci.plugins.pubsub.SimpleMessage();
            msg.set("jenkins_object_type","org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject");
            msg.set("job_run_status","QUEUED");
            msg.set("job_name",jobName);
            msg.set("jenkins_org","jenkins");
            msg.set("job_run_queueId","11");
            msg.set("job_ismultibranch","true");
            msg.set("jenkins_object_name",jobName);
            msg.set("blueocean_job_rest_url","/blue/rest/organizations/jenkins/pipelines/" + orgFolder.getName() + "/pipelines/" + mbp.getName() + "/");
            msg.set("job_multibranch_indexing_status","INDEXING");
            msg.set("jenkins_event","job_run_queue_enter");
            msg.set("blueocean_job_pipeline_name",jobName);
            msg.set("jenkins_object_url","job/" + orgFolder.getName() + "/job/" + mbp.getName() + "/");
            msg.set("jenkins_channel","job");
            PubsubBus.getBus().publish(msg);
        } catch (MessageException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean repoHasJenkinsFile(String apiUrl, String credentialId, String owner, String repo, OrganizationFolder sourceOwner) throws IOException, InterruptedException {
        GitHubSCMSource gitHubSCMSource = new GitHubSCMSource(null, apiUrl, credentialId, credentialId, owner, repo);

        // set OrganizationFolder as owner so that credentials attached to this org folder can be used during github api calls.
        gitHubSCMSource.setOwner(sourceOwner);

        final JenkinsfileCriteria criteria = new JenkinsfileCriteria();
        gitHubSCMSource.fetch(criteria, new SCMHeadObserver() {
            @Override
            public void observe(@Nonnull SCMHead head, @Nonnull SCMRevision revision) throws IOException, InterruptedException {
                //do nothing
            }

            @Override
            public boolean isObserving() {
                //if jenkinsfile is found stop observing
                return !criteria.isJekinsfileFound();

            }
        }, TaskListener.NULL);

        return criteria.isJekinsfileFound();
    }

    static void validateCredentialId(String credentialId,  String apiUrl) throws IOException {
        if (credentialId != null && !credentialId.trim().isEmpty()) {
            StandardUsernamePasswordCredentials credentials = CredentialsUtils.findCredential(credentialId, StandardUsernamePasswordCredentials.class, new BlueOceanDomainRequirement());
            if (credentials == null) {
                throw new ServiceException.BadRequestExpception(new ErrorMessage(400, "Failed to create Github pipeline")
                        .add(new ErrorMessage.Error("scmConfig.credentialId",
                                ErrorMessage.Error.ErrorCodes.NOT_FOUND.toString(),
                                "No Credentials instance found for credentialId: "+credentialId)));
            } else {
                String accessToken = credentials.getPassword().getPlainText();
                validateGithubAccessToken(accessToken, apiUrl);
            }
        }
    }

    private static void deleteOnError(AbstractFolder item){
        try {
            item.delete();
        } catch (InterruptedException | IOException e) {
            throw new ServiceException.UnexpectedErrorException("Failure during cleaning up folder: " + item.getName() + ". Error: " +
                    e.getMessage(), e);
        }

    }

    private static void validateGithubAccessToken(String accessToken, String apiUrl) throws IOException {
        try {
            HttpURLConnection connection =  GithubScm.connect(apiUrl+"/user", accessToken);
            GithubScm.validateAccessTokenScopes(connection);
        } catch (Exception e) {
            if(e instanceof ServiceException){
                throw e;
            }
            throw new ServiceException.UnexpectedErrorException("Failure validating github access token: "+e.getMessage(), e);
        }
    }

    static class SCMSourceEventImpl extends SCMSourceEvent<Object>{
        private final String repoName;
        private final Item project;
        private final GitHubSCMNavigator navigator;

        public SCMSourceEventImpl(String repoName, Item project, String origin, GitHubSCMNavigator navigator) {
            super(Type.CREATED, new Object(), origin);
            this.repoName = repoName;
            this.project=project;
            this.navigator = navigator;
        }

        @Override
        public boolean isMatch(@Nonnull SCMNavigator navigator) {
            return this.navigator == navigator;
        }

        @Override
        public boolean isMatch(@Nonnull SCMSource source) {
            return ((GitHubSCMSource)source).getRepository().equals(getSourceName()) &&
                    source.getOwner().getFullName().equals(project.getFullName());
        }

        @Nonnull
        @Override
        public String getSourceName() {
            return repoName;
        }
    }

    private static class JenkinsfileCriteria implements SCMSourceCriteria{
        private static final long serialVersionUID = 1L;
        private AtomicBoolean jenkinsFileFound = new AtomicBoolean();

            @Override
            public boolean isHead(@Nonnull Probe probe, @Nonnull TaskListener listener) throws IOException {
                    SCMProbeStat stat = probe.stat("Jenkinsfile");
                    boolean foundJekinsFile =  stat.getType() != SCMFile.Type.NONEXISTENT && stat.getType() != SCMFile.Type.DIRECTORY;
                    if(foundJekinsFile && !jenkinsFileFound.get()) {
                        jenkinsFileFound.set(true);
                    }
                    return foundJekinsFile;
            }

            private boolean isJekinsfileFound() {
                return jenkinsFileFound.get();
            }
        }
}
