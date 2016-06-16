var $ = require('jquery-detached').getJQuery();

function getAppUrl() {
    const rootUrl = $('head').attr('data-rooturl');
    if (!rootUrl) {
        return '';
    } else {
        return rootUrl;
    }
}

$(document).ready(() => {
    var tryBlueOcean = $('<div class="try-blueocean header-callout">Try Blue Ocean UI ...</div>');
    
    tryBlueOcean.click(() => {
        // We could enhance this further by looking at the current
        // URL and going to different places in the BO UI depending
        // on where the user is in classic jenkins UI e.g. if they
        // are currently in a job on classic UI, bring them to the
        // same job in BO UI Vs just brining them to the root of
        // BO UI i.e. make the button context sensitive.
        window.location.replace(`${getAppUrl()}/blue`);
    });
    
    $('#page-head #header').append(tryBlueOcean);
});
