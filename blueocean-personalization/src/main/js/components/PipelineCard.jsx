/**
 * Created by cmeyers on 6/28/16.
 */
import React, { Component, PropTypes } from 'react';
import { Link } from 'react-router';
import { capable, UrlBuilder } from '@jenkins-cd/blueocean-core-js';
import { Favorite, LiveStatusIndicator } from '@jenkins-cd/design-language';
import { RunButton, ReplayButton } from '@jenkins-cd/blueocean-core-js';

const stopProp = (event) => {
    event.stopPropagation();
};

const BRANCH_CAPABILITY = 'io.jenkins.blueocean.rest.model.BlueBranch';

/**
 * Extract elements from a path string deliminted with forward slashes
 * @param path
 * @param begin
 * @param end
 * @returns {string}
 */
function extractPath(path, begin, end) {
    try {
        return path.split('/').slice(begin, end).join('/');
    } catch (error) {
        return path;
    }
}

/**
 * Takes a pipeline/branch object and returns the fullName, pipelineName and branchName components
 * @param {object} pipeline
 * @param {boolean} isBranch
 * @returns {{pipelineName: string, fullName: string, branchName: string}}
 * @private
 */
function extractNames(pipeline, isBranch) {
    let fullName = null;
    let pipelineName = null;
    let branchName = null;

    if (isBranch) {
        // pipeline.fullName is in the form folder1/folder2/pipeline/branch ...
        // extract "pipeline"
        pipelineName = extractPath(pipeline.fullName, -2, -1);
        // extract everything up to "branch"
        fullName = extractPath(pipeline.fullName, 0, -1);
        branchName = pipeline.name;
    } else {
        pipelineName = pipeline.name;
        fullName = pipeline.fullName;
    }

    return {
        fullName, pipelineName, branchName,
    };
}

/**
 * PipelineCard displays an informational card about a Pipeline and its status.
 *
 * Properties:
 * router: instance of RouterContext
 * item: pipeline or branch
 * favorite: whether or not the pipeline is favorited
 * onRunClick: callback invoked when 'Run Again' is clicked
 * onFavoriteToggle: callback invokved when favorite checkbox is toggled.
 */
export class PipelineCard extends Component {

    static _getBackgroundClass(status) {
        return status && status.length > 0 ?
            `${status.toLowerCase()}-bg-lite` :
            'unknown-bg-lite';
    }

    constructor(props) {
        super(props);

        this.state = {
            favorite: false,
            stopping: false,
        };
    }

    componentWillMount() {
        this._updateState(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this._updateState(nextProps);
    }

    _navigateToRunDetails() {
        const runUrl = UrlBuilder.buildRunDetailsUrl(this.props.runnable.latestRun);

        this.props.router.push({
            pathname: runUrl,
        });
    }

    _updateState(props) {
        this.setState({
            favorite: props.favorite,
            stopping: false,
        });
    }

    _onRunDetails(url) {
        this.props.router.push(url);
    }

    _onFavoriteToggle() {
        const value = !this.state.favorite;
        this.setState({
            favorite: value,
        });

        if (this.props.onFavoriteToggle) {
            this.props.onFavoriteToggle(value);
        }
    }

    render() {
        if (!this.props.runnable) {
            return null;
        }

        const runnableItem = this.props.runnable;
        const latestRun = this.props.runnable.latestRun;

        const isBranch = capable(runnableItem, BRANCH_CAPABILITY);
        const names = extractNames(runnableItem, isBranch);
        const organization = runnableItem.organization;

        let status;
        let startTime = null;
        let estimatedDuration = null;
        let commitId = null;

        if (latestRun) {
            status = latestRun.result === 'UNKNOWN' ? latestRun.state : latestRun.result;
            startTime = latestRun.startTime;
            estimatedDuration = latestRun.estimatedDurationInMillis;
            commitId = latestRun.commitId;
        } else {
            status = 'NOT_BUILT';
        }

        const bgClass = PipelineCard._getBackgroundClass(status);
        const commitText = commitId ? commitId.substr(0, 7) : '';

        const activityUrl = `/organizations/${encodeURIComponent(organization)}/` +
        `${encodeURIComponent(names.fullName)}/activity`;

        return (
            <div className={`pipeline-card ${bgClass}`} onClick={() => this._navigateToRunDetails()}>
                <LiveStatusIndicator
                  result={status} startTime={startTime} estimatedDuration={estimatedDuration}
                  width={'20px'} height={'20px'} noBackground
                />

                <span className="name">
                    <Link to={activityUrl} onClick={(event) => stopProp(event)}>
                        {organization} / <span title={names.fullName}>{names.pipelineName}</span>
                    </Link>
                </span>

                { isBranch ?
                <span className="branch">
                    <span className="octicon octicon-git-branch"></span>
                    <span className="branchText">{decodeURIComponent(names.branchName)}</span>
                </span>
                :
                <span className="branch"></span>
                }

                { commitId ?
                <span className="commit">
                    <span className="octicon octicon-git-commit"></span>
                    <pre className="commitId">&#35;{commitText}</pre>
                </span>
                :
                <span className="commit"></span>
                }

                <span className="actions">
                    <ReplayButton
                      className="icon-button dark"
                      runnable={runnableItem}
                      latestRun={latestRun}
                      onNavigation={url => this._onRunDetails(url)}
                    />

                    <RunButton
                      className="icon-button dark"
                      runnable={runnableItem}
                      latestRun={latestRun}
                      onNavigation={url => this._onRunDetails(url)}
                    />

                    <Favorite checked={this.state.favorite} className="dark-white"
                      onToggle={() => this._onFavoriteToggle()}
                    />
                </span>
            </div>
        );
    }
}

PipelineCard.propTypes = {
    router: PropTypes.object,
    runnable: PropTypes.object,
    favorite: PropTypes.bool,
    onFavoriteToggle: PropTypes.func,
};

PipelineCard.defaultProps = {
    favorite: false,
};
