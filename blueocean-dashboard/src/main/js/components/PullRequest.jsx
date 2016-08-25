import React, { Component, PropTypes } from 'react';
import { LiveStatusIndicator, ReadableDate } from '@jenkins-cd/design-language';
import Extensions from '@jenkins-cd/js-extensions';
import RunPipeline from './RunPipeline.jsx';
import { buildRunDetailsUrl } from '../util/UrlUtils';

const { object } = PropTypes;

export default class PullRequest extends Component {
    render() {
        const { pr } = this.props;
        if (!pr || !pr.pullRequest || !pr.latestRun || !this.context.pipeline) {
            return null;
        }
        const {
            latestRun: {
                result: resultString,
                id,
                startTime,
                pipeline,
                endTime,
                estimatedDurationInMillis,
                state,
            },
            pullRequest: {
                title,
                author,
            },
            name,
        } = pr;
        const result = resultString === 'UNKNOWN' ? state : resultString;
        const {
            context: {
                router,
                location,
                pipeline: {
                    name: pipelineName,
                    organization,
            },
                },
        } = this;
        const open = () => {
            location.pathname = buildRunDetailsUrl(organization, pipelineName, decodeURIComponent(pipeline), id, 'pipeline');
            router.push(location);
        };

        return (<tr key={id} onClick={open} id={`${name}-${id}`} >
            <td>
                <LiveStatusIndicator result={result} startTime={startTime}
                  estimatedDuration={estimatedDurationInMillis}
                />
            </td>
            <td>{id}</td>
            <td>{title || '-'}</td>
            <td>{author || '-'}</td>
            <td><ReadableDate date={endTime} liveUpdate /></td>
            <td>
                <RunPipeline organization={organization} pipeline={pipelineName} branch={name} />
                <Extensions.Renderer extensionPoint="jenkins.pipeline.pullrequests.list.action" />
            </td>
        </tr>);
    }
}

PullRequest.propTypes = {
    pr: object,
};

PullRequest.contextTypes = {
    pipeline: object,
    router: object.isRequired, // From react-router
    location: object,
};
