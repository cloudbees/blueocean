import React, { PropTypes } from 'react';
import { observer } from 'mobx-react';
import debounce from 'lodash.debounce';

import { Dropdown, FormElement, TextInput } from '@jenkins-cd/design-language';

import FlowStep from '../flow2/FlowStep';
import { CreateCredentialDialog } from '../credentials/CreateCredentialDialog';
import ValidationUtils from '../../util/ValidationUtils';

let t = null;

const NEW_CREDENTIAL_TYPE = {
    SSH_KEY: 'SSH_KEY',
    SYSTEM_SSH: 'SYSTEM_SSH',
    USER_PASS: 'USER_PASS',
    values: () => [
        NEW_CREDENTIAL_TYPE.SSH_KEY,
        NEW_CREDENTIAL_TYPE.USER_PASS,
        NEW_CREDENTIAL_TYPE.SYSTEM_SSH,
    ],
    toLabel(option) {
        switch (option) {
        case NEW_CREDENTIAL_TYPE.SSH_KEY:
            return t('creation.git.step1.credential_type_ssh_key');
        case NEW_CREDENTIAL_TYPE.SYSTEM_SSH:
            return t('creation.git.step1.credential_type_system_ssh');
        case NEW_CREDENTIAL_TYPE.USER_PASS:
            return t('creation.git.step1.credential_type_user_pass');
        default:
            return '';
        }
    },
};

// TODO: HACK! adding a method to allow for programmatic change of Dropdown's selectedOption.
// TODO: needs to be added in the JDL, or handled a little differently via props

Dropdown.prototype.selectOption = function selectOption(option) {
    this.setState({
        selectedOption: option,
    });
};


/**
 * Component that accepts repository URL and credentials to initiate
 * creation of a new pipeline.
 */
@observer
export default class GitConnectStep extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            repositoryUrl: null,
            repositoryErrorMsg: null,
            credentialsErrorMsg: null,
            selectedCredential: null,
            showCreateCredentialDialog: false,
            createButtonDisabled: false,
            createInProgress: false,
        };

        t = this.props.flowManager.translate;
    }

    _bindDropdown(dropdown) {
        this.dropdown = dropdown;
    }

    _repositoryUrlChange(value) {
        this.setState({
            repositoryUrl: value,
        });

        this._updateRepositoryErrorMsg();
    }

    _updateRepositoryErrorMsg = debounce(() => {
        if (this.state.repositoryErrorMsg && ValidationUtils.validateUrl(this.state.repositoryUrl)) {
            this.setState({
                repositoryErrorMsg: null,
            });
        }
    }, 200);

    _selectedCredentialChange(credential) {
        this.setState({
            selectedCredential: credential,
        });
    }

    _onCreateCredentialClick() {
        this.setState({
            showCreateCredentialDialog: true,
        });
    }

    _onCreateCredentialClosed(credential) {
        const newState = {
            showCreateCredentialDialog: false,
        };

        if (credential) {
            newState.selectedCredential = credential;
        }

        this.setState(newState);

        this.dropdown.selectOption(credential);
    }

    _performValidation() {
        if (!ValidationUtils.validateUrl(this.state.repositoryUrl)) {
            this.setState({
                repositoryErrorMsg: t('creation.git.step1.repo_error'),
            });

            return false;
        }

        return true;
    }

    _beginCreation() {
        const isValid = this._performValidation();

        if (!isValid) {
            return;
        }

        this.setState({
            createInProgress: true,
            createButtonDisabled: true,
        });

        this.props.flowManager.createPipeline(this.state.repositoryUrl, this.state.selectedCredential);
    }

    render() {
        const { flowManager } = this.props;

        const disabled = !this.props.flowManager.isConnectEnabled;

        return (
            <FlowStep {...this.props} className="git-step-connect" title={t('creation.git.step1.title')} disabled={disabled}>
                <p className="instructions">
                    {t('creation.git.step1.instructions')} &nbsp;
                    <a href="https://jenkins.io/doc/book/pipeline/jenkinsfile/" target="_blank">{t('creation.git.step1.instructions_link')}</a>
                </p>

                <FormElement title={t('creation.git.step1.repo_title')} errorMessage={this.state.repositoryErrorMsg}>
                    <TextInput className="text-repository-url" onChange={val => this._repositoryUrlChange(val)} />
                </FormElement>

                <FormElement title={t('creation.git.step1.credentials')}>
                    <Dropdown
                      ref={dropdown => this._bindDropdown(dropdown)}
                      className="dropdown-credentials"
                      placeholder={t('creation.git.step1.credentials_placeholder')}
                      options={flowManager.credentials}
                      labelField="displayName"
                      onChange={opt => this._selectedCredentialChange(opt)}
                    />

                    <button
                      className="button-create-credential btn-secondary"
                      onClick={() => this._onCreateCredentialClick()}
                    >
                        {t('creation.git.step1.create_credential_button')}
                    </button>
                </FormElement>

                { this.state.showCreateCredentialDialog &&
                    <CreateCredentialDialog
                      flowManager={flowManager}
                      onClose={cred => this._onCreateCredentialClosed(cred)}
                    />
                }

                <button
                  className="button-create-pipeline"
                  onClick={() => this._beginCreation()}
                  disabled={this.state.createButtonDisabled}
                >
                    {this.state.createInProgress ?
                        t('creation.git.step1.create_button_progress') :
                        t('creation.git.step1.create_button')}
                </button>

            </FlowStep>
        );
    }
}

GitConnectStep.propTypes = {
    flowManager: PropTypes.object,
};
