import jsModules from '@jenkins-cd/js-modules';

/**
 * CSS load tracker.
 * <p/>
 * Keeps track of page CSS, adding and removing CSS as ExtensionPoint components are
 * mounted and unmounted.
 */
export class ResourceLoadTracker {
    constructor() {
        // The CSS resources to be added for each Extension point.
        // Key:     Extension point name.
        // Value:   An array of CSS adjunct URLs that need to be activated when the extension point is rendered.
        this.pointCSSs = {};
    
        // Active CSS.
        // Key:     CSS URL.
        // Value:   Counter of the number of mounted Extension Points that need the CSS to be active.
        //               The onMount and onUnmount functions increment and decrement the counter. When the
        //               counter gets back to zero, the CSS can be removed from the page.
        this.activeCSSs = {};
    }
    
    /**
     * Initialize the loader with the extension point information.
     * @param extensionPointList The Extension point list. An array containing ExtensionPoint
     * metadata for all plugins that define such. It's an aggregation of
     * of the /jenkins-js-extension.json files found on the server classpath.
     */
    setExtensionPointMetadata(extensionPointList) {
        // Reset - for testing.
        this.pointCSSs = {};
        this.activeCSSs = {};
    
        // Iterate through each plugin /jenkins-js-extension.json
        for(var i1 = 0; i1 < extensionPointList.length; i1++) {
            var pluginMetadata = extensionPointList[i1];
            var extensions = pluginMetadata.extensions; // All the extensions defined on the plugin
            var pluginCSS = pluginMetadata.extensionCSS; // The plugin CSS URL (adjunct URL).
    
            // Iterate through the ExtensionPoints defined in each plugin
            for (var i2 = 0; i2 < extensions.length; i2++) {
                var extensionPoint = extensions[i2].extensionPoint; // The extension point name.
                var pointCSS = this.pointCSSs[extensionPoint]; // The current list of CSS URLs for the named extension point.
    
                if (!pointCSS) {
                    pointCSS = [];
                    this.pointCSSs[extensionPoint] = pointCSS;
                }
    
                // Add the plugin CSS if it's not already in the list.
                if (pointCSS.indexOf(pluginCSS) === -1) {
                    pointCSS.push(pluginCSS);
                }
            }
        }
    }
    
    /**
     * Called when a Jenkins ExtensionPoint is mounted.
     * <p/>
     * If the extension point implementations use CSS (comes from plugins that define CSS)
     * then this method will use requireCSS, and then addCSS, for each CSS. addCSS only
     * gets called for a CSS by the first extension point to "require" that CSS.
     *
     * @param extensionPointName The extension point name.
     */
    onMount(extensionPointName) {
        const pointCSS = this.pointCSSs[extensionPointName];
        if (pointCSS) {
            for (var i = 0; i < pointCSS.length; i++) {
                this._requireCSS(pointCSS[i]);
            }
        }
    }
    
    /**
     * Called when a Jenkins ExtensionPoint is unmounted.
     * <p/>
     * If the extension point implementations use CSS (comes from plugins that define CSS)
     * then this method will use unrequireCSS, and then removeCSS, for each CSS. removeCSS only
     * gets called for a CSS by the last extension point to "unrequire" that CSS.
     *
     * @param extensionPointName The extension point name.
     */
    onUnmount(extensionPointName) {
        const pointCSS = this.pointCSSs[extensionPointName];
        if (pointCSS) {
            for (var i = 0; i < pointCSS.length; i++) {
                this._unrequireCSS(pointCSS[i]);
            }
        }
    }

    _requireCSS(url) {
        if (!this.activeCSSs[url]) {
            this._addCSS(url);
            this.activeCSSs[url] = true;
        }
    }

    _unrequireCSS(url) {
        var activeCount = this.activeCSSs[url];
    
        if (!activeCount) {
            // Huh?
            console.warn('Unexpected call to deactivate an inactive Jenkins Extension Point CSS: ' + url);
            // Does this mean that react calls unmount multiple times for a given component instance?
            // That would sound like a bug, no?
        } else {
            activeCount--;
            if (activeCount === 0) {
                // All extension points using this CSS have been unmounted.
                delete this.activeCSSs[url];
                this._removeCSS(url);
            } else {
                this.activeCSSs[url] = activeCount;
            }
        }
    }
    
    _addCSS(url) {
        const cssURLPrefix = jsModules.getAdjunctURL();
        jsModules.addCSSToPage(cssURLPrefix + '/' + url);
    }
    
    _removeCSS(url) {
        const cssURLPrefix = jsModules.getAdjunctURL();
        const cssURL = cssURLPrefix + '/' + url;
        const linkElId = jsModules.toCSSId(cssURL);
        const linkEl = document.getElementById(linkElId);
    
        if (linkEl) {
            linkEl.parentNode.removeChild(linkEl);
        }
    }
}

// in lieu of DI
export const instance = new ResourceLoadTracker();
