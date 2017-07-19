package io.jenkins.blueocean.blueocean_github_pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.TestExtension;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.exceptions.UnirestException;

import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.User;
import io.jenkins.blueocean.rest.factory.organization.OrganizationFactory;
import io.jenkins.blueocean.rest.model.BlueOrganization;
import io.jenkins.blueocean.service.embedded.OrganizationFactoryImpl;
import io.jenkins.blueocean.service.embedded.rest.OrganizationImpl;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import jenkins.model.ModifiableTopLevelItemGroup;


public class GithubOrgFolderPermissionsTest extends GithubMockBase {

    @Test
    public void canCreateWhenHavePermissionsOnDefaultOrg() throws Exception {
        MockAuthorizationStrategy authz = new MockAuthorizationStrategy();
        authz.grant(Jenkins.ADMINISTER).everywhere().to(user);
        j.jenkins.setAuthorizationStrategy(authz);
        // refresh the JWT token otherwise all hell breaks loose.
        jwtToken = getJwtToken(j.jenkins, "vivek", "vivek");
        createGithubOrgFolder(true);
    }

    @Test
    public void canNotCreateWhenHaveNoPermissionOnDefaultOrg() throws Exception {        
        MockAuthorizationStrategy authz = new MockAuthorizationStrategy();
        authz.grant(Item.READ).everywhere().to(user);
        j.jenkins.setAuthorizationStrategy(authz);
        // refresh the JWT token otherwise all hell breaks loose.
        jwtToken = getJwtToken(j.jenkins, "vivek", "vivek");
        createGithubOrgFolder(false);
    }

    @Test
    public void canCreateWhenHavePermissionsOnCustomOrg() throws Exception {
        MockAuthorizationStrategy authz = new MockAuthorizationStrategy();
        authz.grant(Item.READ).everywhere().to(user);
        authz.grant(Item.CREATE, Item.CONFIGURE).onFolders(getOrgRoot()).to(user);
        j.jenkins.setAuthorizationStrategy(authz);
        // refresh the JWT token otherwise all hell breaks loose.
        jwtToken = getJwtToken(j.jenkins, "vivek", "vivek");
        createGithubOrgFolder(true);
    }

    @Test
    public void canNotCreateWhenHaveNoPermissionOnCustomOrg() throws Exception {
        MockAuthorizationStrategy authz = new MockAuthorizationStrategy();
        authz.grant(Item.READ).everywhere().to(user);
        j.jenkins.setAuthorizationStrategy(authz);
        // refresh the JWT token otherwise all hell breaks loose.
        jwtToken = getJwtToken(j.jenkins, "vivek", "vivek");
        createGithubOrgFolder(false);
    }

    private void createGithubOrgFolder(boolean shouldSuceed) throws Exception {
        String credentialId = createGithubCredential(user);
        String orgFolderName = "cloudbeers1";
        Map resp = new RequestBuilder(baseUrl)
                .status(shouldSuceed ? 201 : 403)
                .jwtToken(getJwtToken(j.jenkins,user.getId(), user.getId()))
                .post("/organizations/" + getOrgName() + "/pipelines/")
                .data(ImmutableMap.of("name", orgFolderName,
                        "$class", "io.jenkins.blueocean.blueocean_github_pipeline.GithubPipelineCreateRequest",
                        "scmConfig", ImmutableMap.of("config",
                                ImmutableMap.of("repos", ImmutableList.of("PR-demo"), "orgName","cloudbeers"),
                                "credentialId", credentialId,
                                "uri", githubApiUrl)
                ))
                .build(Map.class);

        TopLevelItem item = getOrgRoot().getItem(orgFolderName);
        if (shouldSuceed) {
            assertEquals(orgFolderName, resp.get("name"));
            assertEquals("io.jenkins.blueocean.blueocean_github_pipeline.GithubOrganizationFolder", resp.get("_class"));

            Assert.assertTrue(item instanceof OrganizationFolder);
            Map r = get("/organizations/"+ getOrgName() + "/pipelines/"+orgFolderName+"/");
            assertEquals(orgFolderName, r.get("name"));
            assertFalse((Boolean) r.get("scanAllRepos"));
        }
        else {
            assertEquals(403, resp.get("code"));
            assertEquals("Failed to create pipeline: cloudbeers1. User vivek doesn't have Job create permission", resp.get("message"));
            Assert.assertNull(item);
            String r = get("/organizations/"+ getOrgName() + "/pipelines/"+orgFolderName+"/", 404, String.class);
        }
    }

    private String createGithubCredential(User user) throws UnirestException {
        Map r = new RequestBuilder(baseUrl)
                .data(ImmutableMap.of("accessToken", "12345"))
                .status(200)
                .jwtToken(getJwtToken(j.jenkins, user.getId(), user.getId()))
                .put("/organizations/" + getOrgName() + "/scm/github/validate/")
                .build(Map.class);

        assertEquals("github", r.get("credentialId"));
        return "github";
    }

    private static String getOrgName() {
        return OrganizationFactory.getInstance().list().iterator().next().getName();
    }

    private static ModifiableTopLevelItemGroup getOrgRoot() {
        return OrganizationFactory.getItemGroup(getOrgName());
    }

    @TestExtension(value={"canCreateWhenHavePermissionsOnCustomOrg","canNotCreateWhenHaveNoPermissionOnCustomOrg"})
    public static class TestOrganizationFactoryImpl extends OrganizationFactoryImpl {

        private OrganizationImpl instance;

        public TestOrganizationFactoryImpl() throws IOException {
            Folder f = Jenkins.getInstance().createProject(Folder.class, "CustomOrg");
            instance = new OrganizationImpl("custom", f);
        }

        @Override
        public OrganizationImpl get(String name) {
            if (instance != null) {
                if (instance.getName().equals(name)) {
                    System.out.println("" + name + " Instance returned " + instance);
                    return instance;
                }
            }
            System.out.println("" + name + " no instance found");
            return null;
        }

        @Override
        public Collection<BlueOrganization> list() {
            return Collections.singleton((BlueOrganization) instance);
        }

        @Override
        public OrganizationImpl of(ItemGroup group) {
            if (group == instance.getGroup()) {
                return instance;
            }
            return null;
        }
    }
}
