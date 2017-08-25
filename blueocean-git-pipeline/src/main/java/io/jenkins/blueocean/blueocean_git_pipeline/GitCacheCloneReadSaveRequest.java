/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
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
package io.jenkins.blueocean.blueocean_git_pipeline;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.model.User;
import hudson.plugins.git.GitException;
import io.jenkins.blueocean.commons.ServiceException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;

import io.jenkins.blueocean.service.embedded.util.UserSSHKeyManager;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

/**
 * Uses the SCM Git cache with a local clone to load/save content
 * @author kzantow
 */
class GitCacheCloneReadSaveRequest extends GitReadSaveRequest {
    private static final String LOCAL_REF_BASE = "refs/heads/";
    private static final String REMOTE_REF_BASE = "refs/heads/";

    public GitCacheCloneReadSaveRequest(GitSCMSource gitSource, String branch, String commitMessage, String sourceBranch, String filePath, byte[] contents) {
        super(gitSource, branch, commitMessage, sourceBranch, filePath, contents);
    }

    @Override
    byte[] read() throws IOException {
        try {
            GitSCMFileSystem fs = getFilesystem();
            return fs.invoke(new GitSCMFileSystem.FSFunction<byte[]>() {
                @Override
                public byte[] invoke(Repository repository) throws IOException, InterruptedException {
                    Git activeRepo = getActiveRepository(repository);
                    Repository repo = activeRepo.getRepository();
                    File repoDir = repo.getDirectory().getParentFile();
                    try {
                        File f = new File(repoDir, filePath);
                        if (f.canRead()) {
                            return IOUtils.toByteArray(new FileInputStream(f));
                        }
                        return null;
                    } finally {
                        FileUtils.deleteDirectory(repoDir);
                    }
                }
            });
        } catch (InterruptedException ex) {
            throw new ServiceException.UnexpectedErrorException("Unable to read " + filePath, ex);
        }
    }

    @Override
    void save() throws IOException {
        try {
            GitSCMFileSystem fs = getFilesystem();
            fs.invoke(new GitSCMFileSystem.FSFunction<Void>() {
                @Override
                public Void invoke(Repository repository) throws IOException, InterruptedException {
                    Git activeRepo = getActiveRepository(repository);
                    Repository repo = activeRepo.getRepository();
                    File repoDir = repo.getDirectory().getParentFile();
                    log.fine("Repo cloned to: " + repoDir.getCanonicalPath());
                    try {
                        File f = new File(repoDir, filePath);
                        if (!f.exists() || f.canWrite()) {
                            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "utf-8")) {
                                w.write(new String(contents, "utf-8"));
                            }

                            try {
                                AddCommand add = activeRepo.add();
                                add.addFilepattern(filePath);
                                add.call();

                                CommitCommand commit = activeRepo.commit();
                                commit.setMessage(commitMessage);
                                commit.call();

                                StandardCredentials credential = null;
                                if (GitUtils.isSshUrl(gitSource.getRemote())) {
                                    // Get committer info and credentials
                                    User user = User.current();
                                    if (user == null) {
                                        throw new ServiceException.UnauthorizedException("Not authenticated");
                                    }
                                    credential = UserSSHKeyManager.getOrCreate(user);
                                }

                                // Push the changes
                                GitUtils.push(gitSource, repo, credential, LOCAL_REF_BASE + sourceBranch, REMOTE_REF_BASE + branch);
                            } catch (GitAPIException ex) {
                                throw new ServiceException.UnexpectedErrorException(ex.getMessage(), ex);
                            }

                            return null;
                        }
                        throw new ServiceException.UnexpectedErrorException("Unable to write " + filePath);
                    } finally {
                        FileUtils.deleteDirectory(repoDir);
                    }
                }
            });
        } catch (InterruptedException ex) {
            throw new ServiceException.UnexpectedErrorException("Unable to save " + filePath, ex);
        }
    }

    @Nonnull GitSCMFileSystem getFilesystem() throws IOException, InterruptedException {
        try {
            GitSCMFileSystem fs = (GitSCMFileSystem) SCMFileSystem.of(gitSource, new SCMHead(branch));
            if (fs == null) {
                throw new ServiceException.NotFoundException("No file found");
            }
            return fs;
        } catch(GitException e) {
            // TODO localization?
            if (e.getMessage().contains("Permission denied")) {
                throw new ServiceException.UnauthorizedException("Not authorized", e);
            }
            throw e;
        }
    }

    private @Nonnull Git getActiveRepository(Repository repository) throws IOException {
        try {
            // Clone the bare repository
            File cloneDir = File.createTempFile("clone", "");

            if (cloneDir.exists()) {
                if (cloneDir.isDirectory()) {
                    FileUtils.deleteDirectory(cloneDir);
                } else {
                    if (!cloneDir.delete()) {
                        throw new ServiceException.UnexpectedErrorException("Unable to delete repository clone");
                    }
                }
            }
            if (!cloneDir.mkdirs()) {
                throw new ServiceException.UnexpectedErrorException("Unable to create repository clone directory");
            }

            String url = repository.getConfig().getString( "remote", "origin", "url" );
            Git gitClient = Git.cloneRepository()
                .setCloneAllBranches(false)
                .setProgressMonitor(new CloneProgressMonitor(url))
                .setURI(repository.getDirectory().getCanonicalPath())
                .setDirectory(cloneDir)
                .call();

            RemoteRemoveCommand remove = gitClient.remoteRemove();
            remove.setName("origin");
            remove.call();

            RemoteAddCommand add = gitClient.remoteAdd();
            add.setName("origin");
            add.setUri(new URIish(gitSource.getRemote()));
            add.call();

            if (GitUtils.isSshUrl(gitSource.getRemote())) {
                // Get committer info and credentials
                User user = User.current();
                if (user == null) {
                    throw new ServiceException.UnauthorizedException("Not authenticated");
                }
                BasicSSHUserPrivateKey privateKey = UserSSHKeyManager.getOrCreate(user);

                // Make sure up-to-date and credentials work
                GitUtils.fetch(repository, privateKey);
            }

            if (!StringUtils.isEmpty(sourceBranch) && !sourceBranch.equals(branch)) {
                CheckoutCommand checkout = gitClient.checkout();
                checkout.setStartPoint("origin/" + sourceBranch);
                checkout.setName(sourceBranch);
                checkout.setCreateBranch(true); // to create a new local branch
                checkout.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK);
                checkout.call();

                checkout = gitClient.checkout();
                checkout.setName(branch);
                checkout.setCreateBranch(true); // this *should* be a new branch
                checkout.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK);
                checkout.call();
            } else {
                CheckoutCommand checkout = gitClient.checkout();
                checkout.setStartPoint("origin/" + branch);
                checkout.setName(branch);
                checkout.setCreateBranch(true); // to create a new local branch
                checkout.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK);
                checkout.call();
            }

            return gitClient;
        } catch (GitAPIException | URISyntaxException ex) {
            throw new ServiceException.UnexpectedErrorException("Unable to get working repository directory", ex);
        }
    }
}
