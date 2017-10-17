import { action, observable } from 'mobx';
import { logging } from '@jenkins-cd/blueocean-core-js';

import { KaraokeApi, KaraokeSpeed } from '../../index';

const logger = logging.logger('io.jenkins.blueocean.dashboard.following.Pager.General');

/**
 * The pager fetches pages of data from the BlueOcean api. It fetches pages of data, then
 * inserts them into the [@link BunkerService], and stores the href from the data.
 *
 * MobX computes a data field from the href backed by the backend cache. This allows for SSE events
 * to be propagated to the pager.
 *
 * @export
 * @class Pager
 */
export class GeneralLogPager {
    /**
     * pager is fetching data. log and detail
     * @type {boolean}
     */
    @observable pending = false;
    /**
     * Will be set in an error occurs.
     * @type {object|null}
     */
    @observable error = null;

    @observable log = null;

    /**
     * Creates an instance of Pager and fetches the first page.
     *
     * @param {BunkerService} bunker - Data store
     * @param {object} pipeline Pipeline that this pager belongs to.
     * @param {string} branch the name of the branch we are requesting
     * @param {string} run Run that this pager belongs to.
     */
    constructor(bunker, pipelineView, location) {
        this.pipelineView = pipelineView;
        const start = location && location.query ? location.query.start : undefined;
        this.fetchGeneralLog({ start, followAlong: pipelineView.following });
    }

    @action
    setLog(log) {
        this.log = log;
    }

    /**
     * Fetches the detail from the backend and set the data
     *
     * @returns {Promise}
     */
    @action
    fetchGeneralLog({ start }) {
        clearTimeout(this.timeout);
        // while fetching we are pending
        this.pending = true;
        // log is text and not json, further it does not has _link in the response
        const logData = {
            _links: {
                self: {
                    href: this.pipelineView.generalLogUrl,
                },
            },
        };
        // get api data and further process it
        return KaraokeApi.getGeneralLog(this.pipelineView.generalLogUrl, { start })
            .then(response => {
                const { newStart, hasMore } = response;
                logger.warn({ newStart, hasMore });
                logData.hasMore = start === '0' ? false : hasMore;
                logData.newStart = newStart;
                return response.text();
            })
            .then(action('Process pager data', text => {
                if (text && text.trim) {
                    logData.data = text.trim().split('\n');
                    this.setLog(logData);
                    logger.debug('saved data', this.pipelineView.generalLogUrl, logData.newStart);
                }
                this.pending = false;
                if (Number(logData.newStart) > 0) {
                    // kill current  timeout if any
                    clearTimeout(this.timeout);
                    // we need to get more input from the log stream
                    this.timeout = setTimeout(() => {
                        this.followGeneralLog(logData);
                    }, KaraokeSpeed);
                }
            })).catch(err => {
                logger.error('Error fetching page', err);
                action('set error', () => { this.error = err; });
            });
    }

    /**
     * Fetches the detail from the backend and set the data
     *
     * @returns {Promise}
     */
    @action
    followGeneralLog(logDataOrg) {
        clearTimeout(this.timeout);
        const logData = { ...logDataOrg };
        return KaraokeApi.getGeneralLog(this.pipelineView.generalLogUrl, { start: logData.newStart })
            .then(action('Process pager data following 1', response => {
                const { newStart, hasMore } = response;
                logger.warn({ newStart, hasMore });
                logData.newStart = response.newStart;
                return response.text();
            }))
            .then(action('Process pager data following 2', text => {
                if (text && text.trim) {
                    const items = text.trim().split('\n');
                    logData.data = logData.data.concat(items);
                    // Store item in bunker.
                    this.bunker.setItem(logData);
                    logger.debug('saved data', this.pipelineView.generalLogUrl, logData.newStart);
                }
                if (logData.newStart !== null) {
                    // kill current  timeout if any
                    // we need to get mpre input from the log stream
                    this.timeout = setTimeout(() => {
                        this.followGeneralLog(logData);
                    }, KaraokeSpeed);
                }
            })).catch(err => {
                logger.error('Error fetching page', err);
                action('set error', () => { this.error = err; });
            });
    }


    clear() {
        clearTimeout(this.timeout);
    }
}
