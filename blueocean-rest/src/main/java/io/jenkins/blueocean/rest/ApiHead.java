package io.jenkins.blueocean.rest;

import hudson.Extension;
import hudson.ExtensionList;
import io.jenkins.blueocean.BlueOceanUI;
import io.jenkins.blueocean.RootRoutable;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.pageable.Pageable;
import io.jenkins.blueocean.rest.pageable.Pageables;
import io.jenkins.blueocean.rest.pageable.PagedResponse;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.GET;

import java.util.HashMap;
import java.util.Map;

/**
 * Entrypoint for blueocean REST apis. $CONTEXT_PATH/rest being root. e.g. /jenkins/rest
 *
 * @author Vivek Pandey
 */
@Extension
public final class ApiHead implements RootRoutable, Reachable  {

    private volatile BlueOceanUI blueOceanUI;

    private volatile Map<String,ApiRoutable> apis;

    /**
     * Search API
     *
     * Gives collection starting from start parameter value with max numbers &lt;= limit
     *
     *
     * @param query query object parsed from value of q URL query parameter
     *
     * @return Gives pageable collection always non-null
     */
    @WebMethod(name="search") @GET @PagedResponse
    public Pageable<?> search(@QueryParameter("q") Query query) {
        for (OmniSearch os : OmniSearch.all()) {
            if (os.getType().equals(query.type))
                return os.search(query);
        }
        return Pageables.empty();
    }

    /**
     * This {@link ApiHead} gets bound to "/rest"
     */
    @Override
    public String getUrlName() {
        return "rest";
    }

    /**
     * Exposes all {@link ApiRoutable}s to URL space.
     *
     * @param route current URL route handled by ApiHead
     * @return {@link ApiRoutable} object
     */
    public ApiRoutable getDynamic(String route) {
        setApis();
        StaplerRequest request = Stapler.getCurrentRequest();
        String m = request.getMethod();
        if(m.equalsIgnoreCase("POST") || m.equalsIgnoreCase("PUT") || m.equalsIgnoreCase("PATCH")) {
            String header = request.getHeader("Content-Type");
            if(header == null || !header.contains("application/json")) {
                throw new ServiceException(415, "Content-Type: application/json required");
            }
        }
        return apis.get(route);
    }

    @Override
    public Link getLink() {
        setBlueOceanUI(); //lazily initialize BlueOceanUI
        return new Link("/"+blueOceanUI.getUrlBase()).rel(getUrlName());
    }

    /**
     * Gives instance of ApiHead by looking in to Extensions. In some cases it might be null, such as when jenkins is
     * booting up.
     */
    public static ApiHead INSTANCE(){
        ExtensionList<ApiHead> extensionList = ExtensionList.lookup(ApiHead.class);
        if(!extensionList.isEmpty()){
            return extensionList.get(0); //ApiHead is singleton
        }

        return null;
    }

    // Lazy initialize BlueOcean UI via injection
    // Fix for: https://issues.jenkins-ci.org/browse/JENKINS-37429
    private void setBlueOceanUI(){
        BlueOceanUI boui = blueOceanUI;
        if(boui == null){
            synchronized (this){
                boui = blueOceanUI;
                if(boui == null){
                    blueOceanUI = boui = Jenkins.getInstance().getInjector().getInstance(BlueOceanUI.class);
                }
            }
        }
    }

    // Lazy initialize ApiRoutable(s), just so we have all of them
    private void setApis(){
        Map<String,ApiRoutable> apiMap = apis;
        if(apiMap == null){
            synchronized (this){
                apiMap = apis;
                if(apiMap == null){
                    Map<String,ApiRoutable> apiMapTmp = new HashMap<>();
                    for ( ApiRoutable api : ExtensionList.lookup(ApiRoutable.class)) {
                        apiMapTmp.put(api.getUrlName(),api);
                    }
                    apis = apiMap = apiMapTmp;
                }
            }
        }
    }
}
