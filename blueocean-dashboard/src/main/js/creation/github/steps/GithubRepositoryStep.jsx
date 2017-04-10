import React, { PropTypes } from 'react';
import { observer } from 'mobx-react';
import { FilterableList } from '@jenkins-cd/design-language';

import FlowStep from '../../flow2/FlowStep';

@observer
export default class GithubRepositoryStep extends React.Component {

    selectRepository(org) {
        this.props.flowManager.selectRepository(org);
    }

    beginCreation() {
        this.props.flowManager.saveSingleRepo();
    }

    _getLoadingMessage() {
        const { repositoriesLoading } = this.props.flowManager;
        const count = this.props.flowManager.repositories.length;

        if (repositoriesLoading) {
            return `Loading Repositories... ${count} so far.`;
        }

        return `Loaded ${count} repositories.`;
    }

    _exit() {
        this.props.flowManager.completeFlow();
    }

    _sortRepos(a, b) {
        return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
    }

    render() {
        const { flowManager } = this.props;
        const title = 'Choose a repository';
        const disabled = flowManager.stepsDisabled;
        const buttonDisabled = !flowManager.selectedRepository;
        const orgName = flowManager.selectedOrganization.name;
        const existingPipelineCount = flowManager.existingPipelineCount;
        const sortedRepos = flowManager.selectableRepositories.slice().sort(this._sortRepos);
        const loading = flowManager.repositoriesLoading;

        return (
            <FlowStep {...this.props} className="github-repo-list-step" title={title} loading={loading} disabled={disabled}>
                <div className="loading-msg">
                    { this._getLoadingMessage()}
                </div>

                { flowManager.existingAutoDiscover &&
                <div>
                    <p className="instructions">
                        The organization "{orgName}"is currently set to "Automatically discover."
                        Changing to "Just one repository" will create one new pipeline and
                        preserve the existing {existingPipelineCount} pipelines.
                    </p>

                    <p className="instructions">
                        Jenkins will no longer actively search for new repositories that contain Jenkinsfiles
                        and create Pipelines for them.
                    </p>
                </div>
                }

                { flowManager.selectableRepositories.length > 0 &&
                <div className="container">
                    <FilterableList
                      className="repo-list"
                      data={sortedRepos}
                      onItemSelect={(idx, repo) => this.selectRepository(repo)}
                      labelFunction={repo => repo.name}
                      filterFunction={(text, repo) => repo.name.toLowerCase().indexOf(text.toLowerCase()) !== -1}
                    />

                    <button
                      className="button-create"
                      onClick={() => this.beginCreation()}
                      disabled={buttonDisabled}
                    >
                        Create Pipeline
                    </button>
                </div>
                }

                { flowManager.repositories.length > 0 && flowManager.selectableRepositories.length === 0 &&
                <div className="container">
                    <p className="instructions">
                        All {flowManager.repositories.length} discovered repositories in the organization
                        "{orgName}" already have Pipelines.
                    </p>

                    <button onClick={() => this._exit()}>Back to Pipelines</button>
                </div>
                }

                { flowManager.repositories.length === 0 &&
                <div className="container">
                    <p className="instructions">
                        The organization "{orgName}" has no repositories.

                        Please pick a different organization or choose "Automatically Discover" instead.
                    </p>

                    <button onClick={() => this._exit()}>Back to Pipelines</button>
                </div>
                }
            </FlowStep>
        );
    }
}

GithubRepositoryStep.propTypes = {
    flowManager: PropTypes.object,
};
