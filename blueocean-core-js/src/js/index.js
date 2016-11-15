/**
 * Created by cmeyers on 8/18/16.
 */

import { Fetch } from './fetch';
import * as sse from '@jenkins-cd/sse-gateway';
import { RunApi } from './rest/RunApi';
import { SseBus } from './sse/SseBus';
import { ToastService } from './ToastService';

export { Fetch, FetchFunctions } from './fetch';
export UrlBuilder from './UrlBuilder';
export UrlConfig from './urlconfig';
export JWT from './jwt';
export TestUtils from './testutils';
export ToastUtils from './ToastUtils';
export Utils from './utils';
export AppConfig from './config';
export Security from './security';

export { ReplayButton } from './components/ReplayButton';
export { RunButton } from './components/RunButton';

// Create and export the SSE connection that will be shared by other
// Blue Ocean components via this package.
export const sseConnection = sse.connect('jenkins-blueocean-core-js');

// export services as a singleton so all plugins will use the same instance

// capabilities
export { capable, capabilityStore, capabilityAugmenter } from './capability/index';

// limit to single instance so that duplicate REST calls aren't made as events come in
const sseBus = new SseBus(sseConnection, Fetch.fetchJSON);
export { sseBus as SseBus };

// required so new toasts are routed to the instance used in blueocean-web
const toastService = new ToastService();
export { toastService as ToastService };

const runApi = new RunApi();
export { runApi as RunApi };
