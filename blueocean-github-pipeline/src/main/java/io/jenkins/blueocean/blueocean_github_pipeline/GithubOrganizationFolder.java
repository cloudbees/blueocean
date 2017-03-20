package io.jenkins.blueocean.blueocean_github_pipeline;

import com.google.common.collect.Iterables;
import hudson.Extension;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.impl.pipeline.OrganizationFolderPipelineImpl;
import jenkins.branch.OrganizationFolder;
import jenkins.scm.api.SCMNavigator;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vivek Pandey
 */
public class GithubOrganizationFolder  extends OrganizationFolderPipelineImpl {
    //Map of repo properties.
    private final Map<String, BlueRepositoryProperty> repos = new HashMap<>();

    public GithubOrganizationFolder(OrganizationFolder folder, Link parent) {
        super(folder, parent);
    }

    @Override
    public boolean isScanAllRepos() {
        if(!getFolder().getSCMNavigators().isEmpty()) {
            SCMNavigator scmNavigator = getFolder().getSCMNavigators().get(0);
            if(scmNavigator instanceof GitHubSCMNavigator){
                GitHubSCMNavigator gitHubSCMNavigator = (GitHubSCMNavigator) scmNavigator;
                return (StringUtils.isBlank(gitHubSCMNavigator.getIncludes()) || gitHubSCMNavigator.getIncludes().equals("*"))
                        && StringUtils.isBlank(gitHubSCMNavigator.getExcludes())
                        && (StringUtils.isBlank(gitHubSCMNavigator.getPattern())
                        || gitHubSCMNavigator.getPattern().equals(".*"));

            }
        }
        return super.isScanAllRepos();
    }

    //TODO: Pull it up in to BlueOrganizationFolder, once we are happy with this abstraction

    /**
     * Client can expect it to return empty map or null. In certain cases, such as when organization folder scan is
     * triggered with only one repo,
     * @return
     */
    @Exported(name = "repos")
    public Map<String, BlueRepositoryProperty> repos(){
        return repos;
    }

    void addRepo(String repo, BlueRepositoryProperty prop){
        repos.put(repo, prop);
    }

    @Extension(ordinal = -8)
    public static class OrganizationFolderFactoryImpl extends OrganizationFolderFactory {
        @Override
        protected OrganizationFolderPipelineImpl getFolder(jenkins.branch.OrganizationFolder folder, Reachable parent) {
            SCMNavigator navigator = Iterables.getFirst(folder.getNavigators(), null);
            return GitHubSCMNavigator.class.isInstance(navigator) ? new GithubOrganizationFolder(folder, parent.getLink()) : null;
        }
    }

    //TODO: Once we are happy with this abstraction, move it up in to blueocean-rest
    @ExportedBean(defaultVisibility = 9999)
    public abstract static class BlueRepositoryProperty{

        /**
         *
         * Tells if this repo meets scan criteria. For example, if a repo has Jenkinsfile in root, it
         * meets the indexing criteria
         */
        @Exported(name = "meetsScanCriteria")
        public abstract boolean meetsIndexingCriteria();
    }
}
