/**
 * This config object comes from blueocean-config.
 */
import { blueocean } from './scopes';

const config = blueocean.config || {};

export default {
    getJenkinsConfig() {
        return config.jenkinsConfig || {};
    },

    getSecurityConfig() {
        return this.getJenkinsConfig().security || {};
    },

    isJWTEnabled() {
        return !!this.getSecurityConfig().enableJWT;
    },

    getLoginUrl() {
        return this.getSecurityConfig().loginUrl;
    },

    /**
     * Set a new "jenkinsConfig" object.
     * Useful for testing in a headless environment.
     * @param newConfig
     * @private
     */
    _setJenkinsConfig(newConfig) {
        config.jenkinsConfig = newConfig;
    },
};
