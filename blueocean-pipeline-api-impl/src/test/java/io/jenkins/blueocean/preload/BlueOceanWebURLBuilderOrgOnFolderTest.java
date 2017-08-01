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
package io.jenkins.blueocean.preload;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.User;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.ApiHead;
import io.jenkins.blueocean.rest.factory.organization.AbstractOrganization;
import io.jenkins.blueocean.rest.factory.organization.OrganizationFactory;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueOrganization;
import io.jenkins.blueocean.rest.model.BluePipelineContainer;
import io.jenkins.blueocean.rest.model.BlueUser;
import io.jenkins.blueocean.rest.model.BlueUserContainer;
import io.jenkins.blueocean.service.embedded.rest.PipelineContainerImpl;
import io.jenkins.blueocean.service.embedded.rest.UserContainerImpl;
import io.jenkins.blueocean.service.embedded.rest.UserImpl;
import jenkins.model.ModifiableTopLevelItemGroup;
import org.acegisecurity.AccessDeniedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.DELETE;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Stapler.class, OrganizationFactory.class })
public class BlueOceanWebURLBuilderOrgOnFolderTest {
    StaplerRequest request;
    @Before
    public void setup() throws IOException {
        TestOrganizationFactoryImpl orgFactory = new TestOrganizationFactoryImpl();
        TestOrganization testOrganization = new TestOrganization("org", "orgDisplayName");
        TestOrganization testOrganization2 = new TestOrganization("org2", "orgDisplayName2");
        orgFactory.testOrganizations = new TestOrganization[] { testOrganization, testOrganization2 };

        request = mock(StaplerRequest.class);

        mockStatic(OrganizationFactory.class);
        when(OrganizationFactory.getInstance()).thenReturn(orgFactory);

        mockStatic(Stapler.class);
        when(Stapler.getCurrentRequest()).thenReturn(request);
    }

    private static final String MATCHER_ORG = ".*\"name\".*\"%s\".*"; //Matches "name":"ORG_NAME"

    @Test
    public void test_get_org_from_url() throws Exception {
        
        OrganizationStatePreloader preloader=new OrganizationStatePreloader();
        
        //If org on url does not match the default one, it should return the matching one
        when(request.getRequestURI()).thenReturn("/jenkins/blue/organizations/org2/some/remaingin/url");

        String state = preloader.getStateJson();
        Assert.assertTrue("Expected detected organization to be 'org2' from the url", state.matches(String.format(MATCHER_ORG, "org2")));

        //If org on url does not exist, should fall back to first one
        when(request.getRequestURI()).thenReturn("/jenkins/blue/organizations/non_existent_org/some/remaingin/url");
        state = preloader.getStateJson();
        Assert.assertTrue("Expected detected organization to be 'org' from the first on the list", state.matches(String.format(MATCHER_ORG, "org")));

        //If the url does not contain an organization, should fall back to first one
        when(request.getRequestURI()).thenReturn("/jenkins/blue/some/remaingin/url");
        state = preloader.getStateJson();
        Assert.assertTrue("Expected detected organization to be 'org' from the first on the list", state.matches(String.format(MATCHER_ORG, "org")));
        
        //If there is no request
        //If the url does not contain an organization, should fall back to first one
        when(request.getRequestURI()).thenReturn(null);
        state = preloader.getStateJson();
        Assert.assertTrue("Expected detected organization to be 'org' from the first on the list", state.matches(String.format(MATCHER_ORG, "org")));
    }

    public static class TestOrganization extends AbstractOrganization implements ModifiableTopLevelItemGroup {
        private final String name;
        private final String displayName;

        private final UserContainerImpl users = new UserContainerImpl(this);

        public TestOrganization(@NonNull String name, @CheckForNull String displayName) {
            this.name = name;
            this.displayName = displayName != null ? displayName : name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Nonnull
        @Override
        public ModifiableTopLevelItemGroup getGroup() {
            return this;
        }

        @Override
        public BluePipelineContainer getPipelines() {
            return new PipelineContainerImpl(this, this,this);
        }

        @WebMethod(name = "")
        @DELETE
        public void delete() {
            throw new ServiceException.NotImplementedException("Not implemented yet");
        }

        @Override
        public BlueUserContainer getUsers() {
            return users;
        }

        @Override
        public BlueUser getUser() {
            User user = User.current();
            if (user == null) {
                throw new ServiceException.NotFoundException("No authenticated user found");
            }
            return new UserImpl(user, new UserContainerImpl(this));
        }

        @Override
        public Link getLink() {
            return ApiHead.INSTANCE().getLink().rel("organizations/" + getName());
        }

        @Override
        public TopLevelItem doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            return null;
        }

        @Override
        public String getFullName() {
            return name;
        }

        @Override
        public String getFullDisplayName() {
            return name;
        }

        @Override
        public Collection<TopLevelItem> getItems() {
            return null;
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUrlChildPrefix() {
            return null;
        }

        @Override
        public TopLevelItem getItem(String name) throws AccessDeniedException {
            return null;
        }

        @Override
        public File getRootDirFor(TopLevelItem child) {
            return null;
        }

        @Override
        public void onRenamed(TopLevelItem item, String oldName, String newName) throws IOException {
        }

        @Override
        public void onDeleted(TopLevelItem item) throws IOException {
        }

        @Override
        public File getRootDir() {
            return null;
        }

        @Override
        public void save() throws IOException {
        }

        @Override
        public <T extends TopLevelItem> T copy(T src, String name) throws IOException {
            return null;
        }

        @Override
        public TopLevelItem createProjectFromXML(String name, InputStream xml) throws IOException {
            return null;
        }

        @Override
        public TopLevelItem createProject(TopLevelItemDescriptor type, String name, boolean notify) throws IOException {
            return null;
        }
    }

    @TestExtension
    public static class TestOrganizationFactoryImpl extends OrganizationFactory {
        TestOrganization[] testOrganizations;

        @Override
        public BlueOrganization get(String name) {
            if (testOrganizations != null) {
                for (TestOrganization org : testOrganizations) {
                    if (org.getName().equals(name)) {
                        return org;
                    }
                }
            }
            return null;
        }

        @Override
        public Collection<BlueOrganization> list() {
            if (testOrganizations != null) {
                return CollectionUtils.arrayToList(testOrganizations);
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public TestOrganization of(ItemGroup group) {
            if (testOrganizations != null) {
                for (TestOrganization org : testOrganizations) {
                    if (group == org.getGroup()) {
                        return org;
                    }
                }
            }
            return null;
        }
    }
}
