package io.jenkins.blueocean.service.embedded;

import io.jenkins.blueocean.api.profile.FindUsersRequest;
import io.jenkins.blueocean.api.profile.FindUsersResponse;
import io.jenkins.blueocean.api.profile.GetOrganizationRequest;
import io.jenkins.blueocean.api.profile.GetOrganizationResponse;
import io.jenkins.blueocean.api.profile.GetUserDetailsRequest;
import io.jenkins.blueocean.api.profile.GetUserDetailsResponse;
import io.jenkins.blueocean.api.profile.GetUserRequest;
import io.jenkins.blueocean.api.profile.GetUserResponse;
import io.jenkins.blueocean.api.profile.ProfileService;
import io.jenkins.blueocean.api.profile.model.User;
import io.jenkins.blueocean.commons.JsonConverter;
import io.jenkins.blueocean.security.Identity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

/**
 * @author Vivek Pandey
 */
public class EmbeddedProfileServiceTest {

    private ProfileService profileService;

    @Rule
    public JenkinsRule j = new JenkinsRule();


    @Before
    public void before(){
        List<ProfileService> profileServices = j.jenkins.getExtensionList(ProfileService.class);
        Assert.assertTrue(profileServices.size() == 1);
        this.profileService = profileServices.get(0);
    }


    @Test
    public void getUserTest() throws Exception {
        GetUserResponse response = profileService.getUser(
                Identity.ANONYMOUS, new GetUserRequest(j.jenkins.getUser("SYSTEM").getId()));

        Assert.assertNotNull(response.user);
        Assert.assertEquals(response.user.id, "SYSTEM");
        Assert.assertEquals(response.user.name, "SYSTEM");
    }

    @Test
    public void getUserDetailsTest() throws Exception {
        GetUserDetailsResponse response = profileService.getUserDetails(
                Identity.ANONYMOUS, new GetUserDetailsRequest((j.jenkins.getUser("alice").getId())));

        Assert.assertNotNull(response.userDetails);
        Assert.assertEquals(response.userDetails.id, j.jenkins.getUser("alice").getId());
        Assert.assertEquals(response.userDetails.name, "alice");
    }

    @Test
    public void getOrganizationTest(){
        GetOrganizationResponse response = profileService.getOrganization(new Identity("alice"),
                new GetOrganizationRequest("jenkins"));
        Assert.assertNotNull(response.organization);
        Assert.assertEquals(response.organization.name, "jenkins");
    }

    @Test
    public void FindUsersTest() throws Exception {
        String[] names = {"alice", "bob"};
        j.jenkins.getUser(names[0]);
        j.jenkins.getUser(names[1]);

        FindUsersResponse response = profileService.findUsers(new Identity("alice"), new FindUsersRequest("jenkins", null, null));

        System.out.println(JsonConverter.toJson(response));
        Assert.assertTrue(response.users.size() == 2);

        for(String name: names){
            boolean found = false;
            for(User user:response.users){
                if(name.equals(user.name)){
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

    }

}
