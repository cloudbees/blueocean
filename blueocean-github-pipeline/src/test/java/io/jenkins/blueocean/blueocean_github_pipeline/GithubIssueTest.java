package io.jenkins.blueocean.blueocean_github_pipeline;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import hudson.model.Job;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.util.DescribableList;
import io.jenkins.blueocean.rest.factory.BlueIssueFactory;
import io.jenkins.blueocean.rest.model.BlueIssue;
import jenkins.branch.MultiBranchProject;
import jenkins.plugins.git.GitSCMSource;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collection;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GithubIssueTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void findIssueKeys() {
        Assert.assertEquals("Find single", Lists.newArrayList("123"), GithubIssue.findIssueKeys("Closed #123"));
        Assert.assertEquals("Find multiple", Lists.newArrayList("123", "143"), GithubIssue.findIssueKeys("Closed #123 and #143"));
        Assert.assertEquals("Do not find alpha", Lists.newArrayList(), GithubIssue.findIssueKeys("#AAA"));
    }

    @Test
    public void jobNotImplemented() throws Exception {
        Job job = mock(Job.class);
        Collection<BlueIssue> resolved = BlueIssueFactory.resolve(job);
        Assert.assertTrue(resolved.isEmpty());
    }

    @Test
    public void changeSetEntry() throws Exception {
        MultiBranchProject project = mock(MultiBranchProject.class);
        Job job = mock(Job.class);
        Run run = mock(Run.class);
        ChangeLogSet logSet = mock(ChangeLogSet.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);

        when(project.getProperties()).thenReturn(new DescribableList(project));
        when(entry.getParent()).thenReturn(logSet);
        when(logSet.getRun()).thenReturn(run);
        when(run.getParent()).thenReturn(job);
        when(job.getParent()).thenReturn(project);

        GitHubSCMSource source = new GitHubSCMSource("foo", null, null, null, "example", "repo");
        when(project.getSCMSources()).thenReturn(Lists.newArrayList(source));

        when(entry.getMsg()).thenReturn("Closed #123 #124");

        Collection<BlueIssue> resolved = BlueIssueFactory.resolve(entry);
        Assert.assertEquals(2, resolved.size());

        Map<String, BlueIssue> issueMap = Maps.uniqueIndex(resolved, new Function<BlueIssue, String>() {
            @Override
            public String apply(BlueIssue input) {
                return input.getId();
            }
        });

        BlueIssue issue123 = issueMap.get("#123");
        Assert.assertEquals("https://github.com/example/repo/issues/123", issue123.getURL());

        BlueIssue issue124 = issueMap.get("#124");
        Assert.assertEquals("https://github.com/example/repo/issues/124", issue124.getURL());
    }

    @Test
    public void changeSetEntryIsNotGithub() throws Exception {
        MultiBranchProject project = mock(MultiBranchProject.class);
        Job job = mock(Job.class);
        Run run = mock(Run.class);
        ChangeLogSet logSet = mock(ChangeLogSet.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);

        when(project.getProperties()).thenReturn(new DescribableList(project));
        when(entry.getParent()).thenReturn(logSet);
        when(logSet.getRun()).thenReturn(run);
        when(run.getParent()).thenReturn(job);
        when(job.getParent()).thenReturn(project);

        when(entry.getMsg()).thenReturn("Closed #123 #124");
        when(project.getSCMSources()).thenReturn(Lists.newArrayList(new GitSCMSource("http://example.com/repo.git")));

        Collection<BlueIssue> resolved = BlueIssueFactory.resolve(entry);
        Assert.assertEquals(0, resolved.size());
    }

    @Test
    public void changeSetJobParentNotMultibranch() throws Exception {
        AbstractFolder project = mock(AbstractFolder.class);
        Job job = mock(Job.class);
        Run run = mock(Run.class);
        ChangeLogSet logSet = mock(ChangeLogSet.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);

        when(project.getProperties()).thenReturn(new DescribableList(project));
        when(entry.getParent()).thenReturn(logSet);
        when(logSet.getRun()).thenReturn(run);
        when(run.getParent()).thenReturn(job);
        when(job.getParent()).thenReturn(project);

        when(entry.getMsg()).thenReturn("Closed #123 #124");

        Collection<BlueIssue> resolved = BlueIssueFactory.resolve(entry);
        Assert.assertEquals(0, resolved.size());
    }
}

