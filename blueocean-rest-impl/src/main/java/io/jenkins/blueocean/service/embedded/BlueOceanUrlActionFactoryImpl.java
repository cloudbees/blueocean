package io.jenkins.blueocean.service.embedded;

import hudson.Extension;
import hudson.model.ModelObject;
import io.jenkins.blueocean.rest.factory.BlueOceanUrlAction;
import io.jenkins.blueocean.rest.factory.BlueOceanUrlActionFactory;

import javax.annotation.Nonnull;

/**
 * @author Vivek Pandey
 */
@Extension(ordinal = -9999)
public class BlueOceanUrlActionFactoryImpl extends BlueOceanUrlActionFactory {
    @Nonnull
    @Override
    public BlueOceanUrlAction get(@Nonnull final ModelObject object) {
        return new BlueOceanUrlActionImpl(object);
    }

}
