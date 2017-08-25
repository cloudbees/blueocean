import React, { PropTypes } from 'react';
import { observer } from 'mobx-react';
import { FormElement } from '@jenkins-cd/design-language';
import { Fetch, AppConfig, getRestUrl } from '@jenkins-cd/blueocean-core-js';
import { Button } from '../../creation/github/Button';
import { i18nTranslator } from '@jenkins-cd/blueocean-core-js';
const t = i18nTranslator('blueocean-dashboard');

function copySelectionText() {
    let copysuccess; // var to check whether execCommand successfully executed
    try {
        copysuccess = document.execCommand('copy'); // copy selected text to clipboard
    } catch (_) {
        copysuccess = false;
    }
    return copysuccess;
}

function clearSelection() {
    if (window.getSelection) {
        window.getSelection().removeAllRanges();
    } else if (document.selection) {
        document.selection.empty();
    }
}

@observer
class GitCredentialsPicker extends React.Component {
    constructor(props) {
        super(props);
        this.state = {};
        this.restOrgPrefix = AppConfig.getRestRoot() + '/organizations/' + AppConfig.getOrganizationName();
    }

    componentWillMount() {
        const { onStatus, dialog, onComplete } = this.props;
        if (onStatus) {
            onStatus('promptLoading');
        }
        Fetch.fetchJSON(this.restOrgPrefix + '/user/publickey/')
        .then(credential => {
            this.setState({ credential: credential });
            if (onStatus) {
                onStatus('promptReady');
            }
            if (!dialog) {
                onComplete(credential);
            }
        });
    }

    copyPublicKeyToClipboard(element) {
        const textBox = this.refs.publicKey;
        textBox.select();
        copySelectionText();
        clearSelection();
        textBox.blur();
    }

    testCredentialAndCloseDialog() {
        const { onComplete, repositoryUrl, pipeline } = this.props;
        const fetchOptions = {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                repositoryUrl,
                pipeline,
                credentialId: this.state.credential.id,
            }),
        };
        this.setState({ connectStatus: { result: 'running' }});
        return Fetch.fetchJSON(this.restOrgPrefix + '/scm/git/validate', { fetchOptions })
        .then(() => {
            this.setState({
                credentialError: null,
                connectStatus: {
                    result: 'success',
                    reset: false,
                }
            });
            onComplete(this.state.credential);
        })
        .catch(error => {
            const message = error.responseBody ? error.responseBody.message : 'An unknown error occurred';
            this.setState({
                credentialError: message && t('creation.git.step1.credentials_publickey_invalid'),
                connectStatus: {
                    result: 'error',
                    reset: true,
                }
            });
        });
    }

    closeDialog() {
        this.context.router.goBack();
    }

    render() {
        if (!this.state.credential) {
            return null;
        }
        return (
            <div className="credentials-picker-git">
                <p className="instructions">
                    {t('creation.git.credentials.register_ssh_key_instructions')}{' '}
                    <a target="jenkins-docs" href="https://jenkins.io/doc/book/blueocean/creating-pipelines/#creating-a-pipeline-for-a-git-repository">learn more</a>.
                </p>
                <FormElement>
                    <textarea className="TextArea-control" ref="publicKey"
                        readOnly={true}
                        onChange={e => e} value={this.state.credential.publickey} />
                </FormElement>
                <a href="javascript:" className="copy-key-link" onClick={() => this.copyPublicKeyToClipboard()}>
                    {t('creation.git.credentials.copy_to_clipboard')}
                </a>
                {this.props.dialog && <FormElement errorMessage={this.state.credentialError} className="action-buttons">
                    <Button status={this.state.connectStatus} onClick={() => this.testCredentialAndCloseDialog()}>{t('creation.git.credentials.connect_and_validate')}</Button>
                    <Button onClick={() => this.closeDialog()} className="btn-secondary">{t('creation.git.create_credential.button_close')}</Button>
                </FormElement>}
            </div>
        );
    }
}

GitCredentialsPicker.propTypes = {
    onStatus: PropTypes.func,
    onComplete: PropTypes.func,
    scmId: PropTypes.string,
    dialog: PropTypes.bool,
    repositoryUrl: PropTypes.string,
    pipeline: PropTypes.object,
};

GitCredentialsPicker.contextTypes = {
    router: React.PropTypes.object,
};

export default GitCredentialsPicker;
