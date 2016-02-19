package io.jenkins.blueocean.api.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jenkins.blueocean.api.profile.model.UserDetails;
import io.jenkins.blueocean.security.Identity;

import javax.annotation.Nullable;

/**
 * Response for {@link ProfileService#getUserDetails(Identity, GetUserDetailsRequest)}
 *
 * @author Vivek Pandey
 */
public final class GetUserDetailsResponse{
    @JsonProperty("user")
    public final UserDetails userDetails;

    public GetUserDetailsResponse(@Nullable @JsonProperty("user")UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}

