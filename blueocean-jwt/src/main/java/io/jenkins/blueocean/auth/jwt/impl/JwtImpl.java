package io.jenkins.blueocean.auth.jwt.impl;

import com.google.common.collect.ImmutableList;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.User;
import hudson.remoting.Base64;
import hudson.tasks.Mailer;
import io.jenkins.blueocean.auth.jwt.JwkService;
import io.jenkins.blueocean.auth.jwt.JwtAuthenticationService;
import io.jenkins.blueocean.auth.jwt.JwtToken;
import io.jenkins.blueocean.commons.ServiceException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.UUID;

/**
 * @author Vivek Pandey
 */
@Extension
public class JwtImpl extends JwtAuthenticationService {

    private static int DEFAULT_EXPIRY_IN_SEC = 1800;
    private static int DEFAULT_MAX_EXPIRY_TIME_IN_MIN = 480;
    private static int DEFAULT_NOT_BEFORE_IN_SEC = 30;

    @Override
    public JwtToken getToken(@Nullable @QueryParameter("expiryTimeInMins") Integer expiryTimeInMins, @Nullable @QueryParameter("maxExpiryTimeInMins") Integer maxExpiryTimeInMins) {
        String t = System.getProperty("EXPIRY_TIME_IN_MINS");
        long expiryTime=DEFAULT_EXPIRY_IN_SEC;
        if(t!= null){
            expiryTime = Integer.parseInt(t);
        }

        int maxExpiryTime = DEFAULT_MAX_EXPIRY_TIME_IN_MIN;

        t = System.getProperty("MAX_EXPIRY_TIME_IN_MINS");
        if(t!= null){
            maxExpiryTime = Integer.parseInt(t);
        }

        if(maxExpiryTimeInMins != null){
            maxExpiryTime = maxExpiryTimeInMins;
        }
        if(expiryTimeInMins != null){
            if(expiryTimeInMins > maxExpiryTime) {
                throw new ServiceException.BadRequestExpception(
                    String.format("expiryTimeInMins %s can't be greated than %s", expiryTimeInMins, maxExpiryTime));
            }
            expiryTime = expiryTimeInMins * 60;
        }

        Authentication authentication = Jenkins.getInstance().getAuthentication();

        if(authentication == null){
            throw new ServiceException.UnauthorizedException("Unauthorized: No login session found");
        }
        String userId = authentication.getName();

        User user = User.get(userId, false, Collections.emptyMap());
        String email = null;
        String fullName = null;
        if(user != null) {
            fullName = user.getFullName();
            userId = user.getId();
            Mailer.UserProperty p = user.getProperty(Mailer.UserProperty.class);
            if(p!=null)
                email = p.getAddress();
        }
        Plugin plugin = Jenkins.getInstance().getPlugin("blueocean-jwt");
        String issuer = "blueocean-jwt:"+ ((plugin!=null) ? plugin.getWrapper().getVersion() : "");

        JwtToken jwtToken = new JwtToken();
        jwtToken.claim.put("jti", UUID.randomUUID().toString().replace("-",""));
        jwtToken.claim.put("iss", issuer);
        jwtToken.claim.put("sub", userId);
        jwtToken.claim.put("name", fullName);
        long currentTime = System.currentTimeMillis()/1000;
        jwtToken.claim.put("iat", currentTime);
        jwtToken.claim.put("exp", currentTime+expiryTime);
        jwtToken.claim.put("nbf", currentTime - DEFAULT_NOT_BEFORE_IN_SEC);

        //set claim
        JSONObject context = new JSONObject();
        JSONObject userObject = new JSONObject();
        userObject.put("id", userId);
        userObject.put("fullName", fullName);
        userObject.put("email", email);
        context.put("user", userObject);
        jwtToken.claim.put("context", context);

        return jwtToken;
    }

    public JwkFactory getJwks(String name) {
        if(name == null){
            throw new ServiceException.BadRequestExpception("keyId is required");
        }

        return new JwkFactory(name);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "BlueOcean Jwt endpoint";
    }

    public class JwkFactory extends JwkService {
        private final String keyId;

        public JwkFactory(String keyId) {
            this.keyId = keyId;
        }

        @Override
        public JSONObject getJwk() {
            JwtToken.JwtRsaDigitalSignatureKey key = new JwtToken.JwtRsaDigitalSignatureKey(keyId);
            try {
                if(!key.exists()){
                    throw new ServiceException.NotFoundException(String.format("kid %s not found", keyId));
                }
            } catch (IOException e) {
                throw new ServiceException.UnexpectedErrorException("Unexpected error: "+e.getMessage(), e);
            }
            RSAPublicKey publicKey = key.getPublicKey();
            JSONObject jwk = new JSONObject();
            jwk.put("kty", "RSA");
            jwk.put("alg","RS256");
            jwk.put("kid",keyId);
            jwk.put("use", "sig");
            jwk.put("key_ops", ImmutableList.of("verify"));
            jwk.put("n", Base64.encode(publicKey.getModulus().toByteArray()));
            jwk.put("e", Base64.encode(publicKey.getPublicExponent().toByteArray()));
            return jwk;
        }
    }

}

