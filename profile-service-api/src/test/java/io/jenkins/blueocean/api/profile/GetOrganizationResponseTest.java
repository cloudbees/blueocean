package io.jenkins.blueocean.api.profile;

import io.jenkins.blueocean.api.profile.model.Organization;
import io.jenkins.blueocean.commons.JsonConverter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vivek Pandey
 */
public class GetOrganizationResponseTest {
    @Test
    public void serializeDeserialize(){
        GetOrganizationResponse response = new GetOrganizationResponse(new Organization("cloudbees"));

        Assert.assertNotNull(response.organization);

        String json = JsonConverter.toJson(response);

        System.out.println("Converted from Java:\n"+json);

        GetOrganizationResponse responseFromJson = JsonConverter.toJava(json, GetOrganizationResponse.class);


        Assert.assertNotNull(responseFromJson.organization);
        Assert.assertEquals(response.organization.name, responseFromJson.organization.name);

        System.out.println("Converted back from Json:\n"+JsonConverter.toJson(responseFromJson));
    }

}
