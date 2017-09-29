package io.jenkins.blueocean.blueocean_github_pipeline;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Job;
import hudson.scm.ChangeLogSet;
import io.jenkins.blueocean.rest.factory.BlueIssueFactory;
import io.jenkins.blueocean.rest.model.BlueIssue;
import jenkins.branch.MultiBranchProject;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubIssue extends BlueIssue {

    private static final Pattern PATTERN = Pattern.compile("((?:[\\w-.]+\\/[\\w-.]+)?#[1-9]\\d*)");

    private final String id;
    private final String url;

    public GithubIssue(String id, String url) {
        this.id = id;
        this.url = url;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getURL() {
        return url;
    }

    @Extension
    public static class FactoryImpl extends BlueIssueFactory {
        @Override
        public Collection<BlueIssue> getIssues(Job job) {
            return null; // Github branches cannot be linked to tickets
        }

        @Override
        public Collection<BlueIssue> getIssues(ChangeLogSet.Entry changeSetEntry) {
            Job job = changeSetEntry.getParent().getRun().getParent();
            if (!(job.getParent() instanceof MultiBranchProject)) {
                return null;
            }
            MultiBranchProject mbp = (MultiBranchProject)job.getParent();
            List sources = mbp.getSCMSources();
            if (sources.isEmpty() || !(sources.get(0) instanceof GitHubSCMSource)) {
                return null;
            }
            GitHubSCMSource source = (GitHubSCMSource) sources.get(0);
            final String repositoryUri = source.getUriResolver().getRepositoryUri(source.getApiUri(), source.getRepoOwner(), source.getRepository());
            return Collections2.transform(findIssueKeys(changeSetEntry.getMsg()), new Function<String, BlueIssue>() {
                @Override
                public BlueIssue apply(String input) {
                    // Remove ".git"
                    String uri = repositoryUri.substring(0, repositoryUri.length() - 4);
                    return new GithubIssue("#" + input, String.format("%s/issues/%s", uri, input));
                }
            });
        }
    }

    static Collection<String> findIssueKeys(String input) {
        Collection<String> ids = Lists.newArrayList();
        Matcher m = PATTERN.matcher(input);
        while (m.find()) {
            if (m.groupCount() >= 1) {
                String issue = m.group(1);
                ids.add(issue.substring(1, issue.length()));
            }
        }
        return ids;
    }
}
