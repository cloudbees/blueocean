package io.jenkins.blueocean.blueocean_github_pipeline;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.exceptions.UnirestException;
import hudson.model.User;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineBaseTest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Rule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

/**
 * @author Vivek Pandey
 */
public abstract class GithubMockBase extends PipelineBaseTest {
    protected String githubApiUrl;
    protected User user;
    protected String accessToken = "12345";

    @Rule
    public WireMockRule githubApi = new WireMockRule(wireMockConfig().
            dynamicPort().dynamicHttpsPort()
            .usingFilesUnderClasspath("api")
            .extensions(
                    new ResponseTransformer() {
                        @Override
                        public Response transform(Request request, Response response, FileSource files,
                                                  Parameters parameters) {
                            if ("application/json"
                                    .equals(response.getHeaders().getContentTypeHeader().mimeTypePart())) {
                                return Response.Builder.like(response)
                                        .but()
                                        .body(response.getBodyAsString()
                                                .replace("https://api.github.com/",
                                                        "http://localhost:" + githubApi.port() + "/")
                                        )
                                        .build();
                            }
                            return response;
                        }

                        @Override
                        public String getName() {
                            return "url-rewrite";
                        }

                    })
    );

    private final List<StubMapping> perTestStubMappings = new ArrayList<>();

    @Override
    public void setup() throws Exception {
        super.setup();
        //setup github api mock with WireMock
        new File("src/test/resources/api/mappings").mkdirs();
        new File("src/test/resources/api/__files").mkdirs();
        githubApi.enableRecordMappings(new SingleRootFileSource("src/test/resources/api/mappings"),
                new SingleRootFileSource("src/test/resources/api/__files"));
        githubApi.stubFor(
                WireMock.get(urlMatching(".*")).atPriority(10).willReturn(aResponse().proxiedFrom("https://api.github.com/")));

        this.user = login("vivek", "Vivek Pandey", "vivek.pandey@gmail.com");
        this.githubApiUrl = String.format("http://localhost:%s",githubApi.port());
    }

    @After
    public void tearDown() {
        if (!perTestStubMappings.isEmpty()) {
            for (StubMapping mapping : perTestStubMappings) {
                githubApi.removeStub(mapping);
            }

            perTestStubMappings.clear();
        }
    }

    protected String createGithubCredential() throws UnirestException {
        Map r = new RequestBuilder(baseUrl)
                .data(ImmutableMap.of("accessToken", accessToken))
                .status(200)
                .jwtToken(getJwtToken(j.jenkins, user.getId(), user.getId()))
                .put("/organizations/jenkins/scm/github/validate/?apiUrl="+githubApiUrl)
                .build(Map.class);
        String credentialId = (String) r.get("credentialId");
        assertEquals("github", credentialId);
        return credentialId;
    }

    protected String createGithubEnterpriseCredential() throws UnirestException {
        Map r = new RequestBuilder(baseUrl)
            .data(ImmutableMap.of("accessToken", accessToken))
            .status(200)
            .jwtToken(getJwtToken(j.jenkins, user.getId(), user.getId()))
            .put("/organizations/jenkins/scm/github-enterprise/validate/?apiUrl="+githubApiUrl)
            .build(Map.class);
        String credentialId = (String) r.get("credentialId");
        assertEquals(GithubEnterpriseScm.ID+":"+ getGithubApiUrlEncoded(), credentialId);
        return credentialId;
    }

    protected String getGithubApiUrlEncoded() {
        return DigestUtils.sha256Hex(githubApiUrl);
    }

    /**
     * Add a StubMapping to Wiremock corresponding to the supplied builder.
     * Any mappings added will automatically be removed when @After fires.
     * @param builder
     */
    protected void addPerTestStub(MappingBuilder builder) {
        StubMapping mapping = githubApi.stubFor(builder);
        perTestStubMappings.add(mapping);
    }
}
