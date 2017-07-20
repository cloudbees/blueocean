package io.jenkins.blueocean.blueocean_bitbucket_pipeline.cloud;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.jenkins.blueocean.blueocean_bitbucket_pipeline.BitbucketApi.X_BB_API_TEST_MODE_HEADER;
import static org.junit.Assert.*;

/**
 * @author Vivek Pandey
 */
public class BitbucketCloudScmTest extends BbCloudWireMock {
    @Test
    public void getBitbucketScm() throws IOException, UnirestException {
        Map r = new RequestBuilder(baseUrl)
                .status(200)
                .jwtToken(getJwtToken(j.jenkins, authenticatedUser.getId(), authenticatedUser.getId()))
                .get("/organizations/jenkins/scm/"+ BitbucketCloudScm.ID+getApiUrlParam())
                .build(Map.class);

        assertNotNull(r);
        assertEquals(BitbucketCloudScm.ID, r.get("id"));
        assertEquals(apiUrl, r.get("uri"));
        assertNull(r.get("credentialId"));
    }

    @Test
    public void getOrganizationsWithoutCredentialId() throws IOException, UnirestException {
        Map r = new RequestBuilder(baseUrl)
                .status(400)
                .jwtToken(getJwtToken(j.jenkins, authenticatedUser.getId(), authenticatedUser.getId()))
                .get("/organizations/jenkins/scm/"+ BitbucketCloudScm.ID+"/organizations/"+getApiUrlParam())
                .build(Map.class);

    }

    @Test
    public void getOrganizations() throws IOException, UnirestException {
        String credentialId = createCredential(BitbucketCloudScm.ID, "cloud");
        List orgs = new RequestBuilder(baseUrl)
                .status(200)
                .jwtToken(getJwtToken(j.jenkins, authenticatedUser.getId(), authenticatedUser.getId()))
                .get("/organizations/jenkins/scm/"+BitbucketCloudScm.ID+"/organizations/"+getApiUrlParam()+"&credentialId="+credentialId)
                .header(X_BB_API_TEST_MODE_HEADER, "cloud")
                .build(List.class);
        assertEquals(2, orgs.size());
        assertEquals("vivekp7", ((Map)orgs.get(0)).get("key"));
        assertEquals("Vivek Pandey", ((Map)orgs.get(0)).get("name"));
        assertEquals("vivektestteam", ((Map)orgs.get(1)).get("key"));
        assertEquals("Vivek's Team", ((Map)orgs.get(1)).get("name"));
    }

    @Test
    public void getRepositories() throws IOException, UnirestException {
        String credentialId = createCredential(BitbucketCloudScm.ID, "cloud");
        Map repoResp = new RequestBuilder(baseUrl)
                .status(200)
                .jwtToken(getJwtToken(j.jenkins, authenticatedUser.getId(), authenticatedUser.getId()))
                .header(X_BB_API_TEST_MODE_HEADER, "cloud")
                .get("/organizations/jenkins/scm/"+BitbucketCloudScm.ID+"/organizations/vivektestteam/repositories/"+getApiUrlParam()+"&credentialId="+credentialId)
                .build(Map.class);
        List repos = (List) ((Map)repoResp.get("repositories")).get("items");
        assertEquals("pipeline-demo-test", ((Map)repos.get(0)).get("name"));
        assertEquals("pipeline-demo-test", ((Map)repos.get(0)).get("description"));
        assertTrue((Boolean) ((Map)repos.get(0)).get("private"));
        assertEquals("master",((Map)repos.get(0)).get("defaultBranch"));

        assertEquals(2, repos.size());
        assertEquals("emptyrepo", ((Map)repos.get(1)).get("name"));
        assertEquals("emptyrepo", ((Map)repos.get(1)).get("description"));
        assertTrue((Boolean) ((Map)repos.get(1)).get("private"));
        assertNull(((Map)repos.get(1)).get("defaultBranch"));
    }

    private String getApiUrlParam(){
        return String.format("?apiUrl=%s",apiUrl);
    }
}
