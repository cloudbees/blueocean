import React, { Component, PropTypes } from 'react';
import {
    CommitHash, ReadableDate, LiveStatusIndicator, TimeDuration,
}
    from '@jenkins-cd/design-language';
import { ReplayButton, RunButton, UrlConfig } from '@jenkins-cd/blueocean-core-js';

import { MULTIBRANCH_PIPELINE, SIMPLE_PIPELINE } from '../Capabilities';

import Extensions from '@jenkins-cd/js-extensions';
import moment from 'moment';
import { buildRunDetailsUrl } from '../util/UrlUtils';
import IfCapability from './IfCapability';

/*
 http://localhost:8080/jenkins/blue/rest/organizations/jenkins/pipelines/PR-demo/runs
 */
export default class Runs extends Component {
    constructor(props) {
        super(props);
        this.state = { isVisible: false };
    }
    render() {
        // early out
        if (!this.props.result || !this.props.pipeline) {
            return null;
        }
        const {
            context: {
                router,
                location,
            },
            props: {
                changeset,
                locale,
                result: {
                    durationInMillis,
                    estimatedDurationInMillis,
                    pipeline,
                    id,
                    result,
                    state,
                    startTime,
                    endTime,
                    commitId,
                },
                t,
                pipeline: {
                    _class: pipelineClass,
                    fullName,
                    organization,
                },
            },
        } = this;

        const resultRun = result === 'UNKNOWN' ? state : result;
        const running = resultRun === 'RUNNING';
        const durationMillis = !running ?
            durationInMillis :
            moment().diff(moment(startTime));

        const pipelineName = decodeURIComponent(pipeline);
        const runDetailsUrl = buildRunDetailsUrl(organization, fullName, pipelineName, id, 'pipeline');
           
        const open = (event) => {
            if (event) {
                event.preventDefault();
            }
            location.pathname = runDetailsUrl;
            router.push(location);
        };
        const RunCol = (props) => <td className="tableRowLink">
            <a onClick={open} href={`${UrlConfig.getJenkinsRootURL()}/blue${runDetailsUrl}`}>{props.children}</a>
        </td>;
        
        const openRunDetails = (newUrl) => {
            location.pathname = newUrl;
            router.push(location);
        };
        return (<tr key={id} onClick={open} id={`${pipeline}-${id}`} >
            <RunCol>
                <LiveStatusIndicator result={resultRun} startTime={startTime}
                  estimatedDuration={estimatedDurationInMillis}
                />
            </RunCol>
            <RunCol>{id}</RunCol>
            <RunCol><CommitHash commitId={commitId} /></RunCol>
            <IfCapability className={pipelineClass} capability={MULTIBRANCH_PIPELINE} >
                <RunCol>{decodeURIComponent(pipeline)}</RunCol>
            </IfCapability>
            <RunCol>{changeset && changeset.msg || '-'}</RunCol>
            <RunCol>
                <TimeDuration
                  millis={durationMillis}
                  liveUpdate={running}
                  locale={locale}
                  liveFormat={t('common.date.duration.format', { defaultValue: 'm[ minutes] s[ seconds]' })}
                  hintFormat={t('common.date.duration.hint.format', { defaultValue: 'M [month], d [days], h[h], m[m], s[s]' })}
                />
            </RunCol>
            <RunCol>
                <ReadableDate
                  date={endTime}
                  liveUpdate
                  locale={locale}
                  shortFormat={t('common.date.readable.short', { defaultValue: 'MMM DD h:mma Z' })}
                  longFormat={t('common.date.readable.long', { defaultValue: 'MMM DD YYYY h:mma Z' })}
                />
            </RunCol>
             <td>
                <Extensions.Renderer extensionPoint="jenkins.pipeline.activity.list.action" {...t} />
                <RunButton className="icon-button" runnable={this.props.pipeline} latestRun={this.props.run} buttonType="stop-only" />
                { /* TODO: check can probably removed and folded into ReplayButton once JENKINS-37519 is done */ }
                <IfCapability className={pipelineClass} capability={[MULTIBRANCH_PIPELINE, SIMPLE_PIPELINE]}>
                    <ReplayButton className="icon-button" runnable={this.props.pipeline} latestRun={this.props.run} onNavigation={openRunDetails} />
                </IfCapability>
            </td>
        </tr>);
    }
}

const { object, string, any, func } = PropTypes;

Runs.propTypes = {
    run: object,
    pipeline: object,
    result: any.isRequired, // FIXME: create a shape
    data: string,
    locale: string,
    changeset: object.isRequired,
    t: func,
};
Runs.contextTypes = {
    router: object.isRequired, // From react-router
    location: object,
};
