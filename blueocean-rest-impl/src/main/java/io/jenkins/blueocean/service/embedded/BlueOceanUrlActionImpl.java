package io.jenkins.blueocean.service.embedded;

import hudson.Extension;
import hudson.model.ModelObject;
import io.jenkins.blueocean.rest.factory.BlueOceanUrlAction;
import io.jenkins.blueocean.rest.factory.BlueOceanUrlMapper;

import javax.annotation.Nonnull;

/**
 * @author Vivek Pandey
 */
@Extension(ordinal = -9999)
public class BlueOceanUrlActionImpl implements BlueOceanUrlAction {
    private final ModelObject modelObject;

    public BlueOceanUrlActionImpl(@Nonnull ModelObject modelObject) {
        this.modelObject = modelObject;
    }

    public BlueOceanUrlActionImpl() {
        this.modelObject = null;
    }

    @Nonnull
    @Override
    public String getUrl() {
        if(modelObject != null) {
            for(BlueOceanUrlMapper mapper: BlueOceanUrlMapper.all()){
                String url = mapper.getUrl(modelObject);
                if(url != null){
                    return url;
                }
            }
        }
        return BlueOceanUrlMapperImpl.getLandingPagePath();
    }

    @Override
    public String getIconFileName() {
        return "/plugin/blueocean-rest-impl/images/48x48/blueocean.png";
    }

    @Override
    public String getDisplayName() {
        return Messages.BlueOceanUrlActionImpl_DisplayName();
    }

    @Override
    public String getUrlName() {
        return getUrl();
    }
}
