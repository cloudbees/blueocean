package io.jenkins.blueocean.blueocean_git_pipeline;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.Cause;
import hudson.model.Failure;
import hudson.model.TopLevelItem;
import hudson.model.User;
import io.jenkins.blueocean.commons.ErrorMessage;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.impl.pipeline.MultiBranchPipelineImpl;
import io.jenkins.blueocean.rest.impl.pipeline.credential.BlueOceanCredentialsProvider;
import io.jenkins.blueocean.rest.impl.pipeline.credential.BlueOceanDomainRequirement;
import io.jenkins.blueocean.rest.impl.pipeline.credential.CredentialsUtils;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.BlueScmConfig;
import io.jenkins.blueocean.service.embedded.rest.AbstractPipelineCreateRequestImpl;
import jenkins.branch.BranchSource;
import jenkins.branch.MultiBranchProjectDescriptor;
import jenkins.model.Jenkins;
import jenkins.plugins.git.GitSCMSource;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vivek Pandey
 */
public class GitPipelineCreateRequest extends AbstractPipelineCreateRequestImpl {

    private static final String MODE = "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject";
    private static final Logger logger = LoggerFactory.getLogger(GitPipelineCreateRequest.class);

    private BlueScmConfig scmConfig;

    @DataBoundConstructor
    public GitPipelineCreateRequest(String name, BlueScmConfig scmConfig) {
        validate(name, scmConfig);
        setName(name);
        this.scmConfig = scmConfig;
    }

    @Override
    public BluePipeline create(Reachable parent) throws IOException {
        User authenticatedUser =  User.current();
        if(authenticatedUser == null){
            throw new ServiceException.UnauthorizedException("Must login to create a pipeline");
        }

        String sourceUri = scmConfig.getUri();

        if (sourceUri == null) {
            throw new ServiceException.BadRequestExpception(new ErrorMessage(400, "Failed to create Git pipeline:"+getName())
                    .add(new ErrorMessage.Error("scmConfig.uri", ErrorMessage.Error.ErrorCodes.MISSING.toString(), "uri is required")));
        }

        TopLevelItem item = create(Jenkins.getInstance(), getName(), MODE, MultiBranchProjectDescriptor.class);

        if (item instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject project = (WorkflowMultiBranchProject) item;

            if(StringUtils.isNotBlank(scmConfig.getCredentialId())) {
                Domain domain = CredentialsUtils.findDomain(scmConfig.getCredentialId(), authenticatedUser);
                if(domain == null){
                    throw new ServiceException.BadRequestExpception(
                            new ErrorMessage(400, "Failed to create pipeline")
                                    .add(new ErrorMessage.Error("scm.credentialId",
                                            ErrorMessage.Error.ErrorCodes.INVALID.toString(),
                                            "No domain in user credentials found for credentialId: "+ scmConfig.getCredentialId())));
                }
                if(domain.test(new BlueOceanDomainRequirement())) { //this is blueocean specific domain
                    project.addProperty(
                            new BlueOceanCredentialsProvider.FolderPropertyImpl(authenticatedUser.getId(),
                                    scmConfig.getCredentialId(),
                                    BlueOceanCredentialsProvider.createDomain(sourceUri)));
                }
            }

            String credentialId = StringUtils.defaultString(scmConfig.getCredentialId());

            project.getSourcesList().add(new BranchSource(new GitSCMSource(null, sourceUri, credentialId, "*", "", false)));
            project.scheduleBuild(new Cause.UserIdCause());
            return new MultiBranchPipelineImpl(project);
        } else {
            try {
                item.delete(); // we don't know about this project type
            } catch (InterruptedException e) {
                throw new ServiceException.UnexpectedErrorException("Failed to delete pipeline: " + getName());
            }
        }
        return null;
    }

    private void validate(String name, BlueScmConfig scmConfig){
        if(scmConfig == null){
            throw new ServiceException.BadRequestExpception(new ErrorMessage(400, "Failed to create Git pipeline")
                    .add(new ErrorMessage.Error("scmConfig", ErrorMessage.Error.ErrorCodes.MISSING.toString(), "scmConfig is required")));
        }

        List<ErrorMessage.Error> errors = new ArrayList<>();

        String sourceUri = scmConfig.getUri();

        if (sourceUri == null) {
            errors.add(new ErrorMessage.Error("scmConfig.uri", ErrorMessage.Error.ErrorCodes.MISSING.toString(), "uri is required"));
        }else {
            StandardCredentials credentials = null;
            if(scmConfig.getCredentialId() != null){
                credentials = GitUtils.getCredentials(Jenkins.getInstance(), sourceUri, scmConfig.getCredentialId());
                if (credentials == null) {
                    errors.add(new ErrorMessage.Error("scmConfig.credentialId",
                                    ErrorMessage.Error.ErrorCodes.NOT_FOUND.toString(),
                                    String.format("credentialId: %s not found", scmConfig.getCredentialId())));
                }
            }
            //validate credentials if no credential id (perhaps git repo doesn't need auth or credentials is present)
            if(scmConfig.getCredentialId() == null || credentials != null) {
                errors.addAll(GitUtils.validateCredentials(sourceUri, credentials));
            }
        }

        try {
            Jenkins.getInstance().getProjectNamingStrategy().checkName(getName());
        }catch (Failure f){
            errors.add(new ErrorMessage.Error("scmConfig.name", ErrorMessage.Error.ErrorCodes.INVALID.toString(), name + "in not a valid name"));
        }

        if(Jenkins.getInstance().getItem(name)!=null) {
            errors.add(new ErrorMessage.Error("name",
                    ErrorMessage.Error.ErrorCodes.ALREADY_EXISTS.toString(), name + " already exists"));
        }

        if(!errors.isEmpty()){
            throw new ServiceException.BadRequestExpception(new ErrorMessage(400, "Failed to create Git pipeline:"+name).addAll(errors));
        }
    }

}
