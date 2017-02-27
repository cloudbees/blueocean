import { capabilityAugmenter, capable, Fetch, UrlConfig, Utils } from '@jenkins-cd/blueocean-core-js';
import { Enum } from '../../flow2/Enum';

const INVALID_ACCESS_TOKEN_CODE = 428;
const INVALID_ACCESS_TOKEN_MSG = 'Invalid Github accessToken';
const INVALID_SCOPES_MSG = 'Github accessToken does not have required scopes';

export const ListOrganizationsOutcome = new Enum({
    SUCCESS: 'success',
    INVALID_TOKEN_REVOKED: 'revoked_token',
    INVALID_TOKEN_SCOPES: 'invalid_token_scopes',
    ERROR: 'error',
});


/**
 * Handles lookup of Github orgs and repos, and saving of the Github org folder.
 */
export class GithubCreationApi {

    constructor(fetch) {
        this._fetch = fetch || Fetch.fetchJSON;
    }

    listOrganizations(credentialId) {
        const path = UrlConfig.getJenkinsRootURL();
        const orgsUrl = Utils.cleanSlashes(`${path}/blue/rest/organizations/jenkins/scm/github/organizations/?credentialId=${credentialId}`, false);

        return this._fetch(orgsUrl)
            .then(orgs => capabilityAugmenter.augmentCapabilities(orgs))
            .then(
                orgs => this._listOrganizationsSuccess(orgs),
                error => this._listOrganizationsFailure(error),
            );
    }

    _listOrganizationsSuccess(organizations) {
        return {
            outcome: ListOrganizationsOutcome.SUCCESS,
            organizations,
        };
    }

    _listOrganizationsFailure(error) {
        const { code, message } = error.responseBody;

        if (code === INVALID_ACCESS_TOKEN_CODE) {
            if (message.indexOf(INVALID_ACCESS_TOKEN_MSG) !== -1) {
                return {
                    outcome: ListOrganizationsOutcome.INVALID_TOKEN_REVOKED,
                };
            }

            if (message.indexOf(INVALID_SCOPES_MSG) !== -1) {
                return {
                    outcome: ListOrganizationsOutcome.INVALID_TOKEN_SCOPES,
                };
            }
        }

        return {
            outcome: ListOrganizationsOutcome.ERROR,
            error: message,
        };
    }

    listRepositories(credentialId, organizationName, pageNumber = 1, pageSize = 100) {
        const path = UrlConfig.getJenkinsRootURL();
        const reposUrl = Utils.cleanSlashes(
            `${path}/blue/rest/organizations/jenkins/scm/github/organizations/${organizationName}/repositories/` +
            `?credentialId=${credentialId}&pageNumber=${pageNumber}&pageSize=${pageSize}`);

        return this._fetch(reposUrl)
            .then(response => capabilityAugmenter.augmentCapabilities(response));
    }

    findExistingOrgFolder(organization) {
        const path = UrlConfig.getJenkinsRootURL();
        const orgFolderUrl = Utils.cleanSlashes(`${path}/blue/rest/organizations/jenkins/pipelines/${organization.name}`);
        return this._fetch(orgFolderUrl)
            .then(response => capabilityAugmenter.augmentCapabilities(response))
            .then(
                data => this._findExistingOrgFolderSuccess(data),
                error => this._findExistingOrgFolderFailure(error),
            );
    }

    _findExistingOrgFolderSuccess(orgFolder) {
        const isOrgFolder = capable(orgFolder, 'jenkins.branch.OrganizationFolder');

        return {
            isFound: true,
            isOrgFolder,
            orgFolder,
        };
    }

    _findExistingOrgFolderFailure() {
        return {
            isFound: false,
            isOrgFolder: false,
        };
    }

    findExistingOrgFolderPipeline(pipelineName) {
        const path = UrlConfig.getJenkinsRootURL();
        const pipelineUrl = Utils.cleanSlashes(`${path}/blue/rest/organizations/jenkins/pipelines/${pipelineName}`);
        return this._fetch(pipelineUrl)
            .then(response => capabilityAugmenter.augmentCapabilities(response))
            .then(
                pipeline => this._findExistingOrgFolderPipelineSuccess(pipeline),
                () => this._findExistingOrgFolderPipelineFailure(),
            );
    }

    _findExistingOrgFolderPipelineSuccess(pipeline) {
        return {
            isFound: true,
            pipeline,
        };
    }

    _findExistingOrgFolderPipelineFailure() {
        return {
            isFound: false,
        };
    }

    createOrgFolder(credentialId, organization, repoNames = []) {
        const path = UrlConfig.getJenkinsRootURL();
        const createUrl = Utils.cleanSlashes(`${path}/blue/rest/organizations/jenkins/pipelines/`);

        const requestBody = this._buildRequestBody(
            true, credentialId, organization.name, organization.name, repoNames,
        );

        const fetchOptions = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody),
        };

        return this._fetch(createUrl, { fetchOptions })
            .then(pipeline => capabilityAugmenter.augmentCapabilities(pipeline));
    }

    updateOrgFolder(credentialId, orgFolder, repoNames = []) {
        const path = UrlConfig.getJenkinsRootURL();
        const { href } = orgFolder._links.self;
        const updateUrl = Utils.cleanSlashes(`${path}/${href}`);

        const requestBody = this._buildRequestBody(
            false, credentialId, orgFolder.name, orgFolder.name, repoNames,
        );

        const fetchOptions = {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody),
        };

        return this._fetch(updateUrl, { fetchOptions })
            .then(pipeline => capabilityAugmenter.augmentCapabilities(pipeline));
    }

    _buildRequestBody(create, credentialId, itemName, organizationName, repoNames) {
        const className = create ?
            'GithubPipelineCreateRequest' :
            'GithubPipelineUpdateRequest';

        return {
            name: itemName,
            $class: `io.jenkins.blueocean.blueocean_github_pipeline.${className}`,
            scmConfig: {
                credentialId,
                uri: 'https://api.github.com', // optional for github! required for enterprise where it should be http://ghe.acme.com/api/v3
                config: {
                    orgName: organizationName,
                    repos: repoNames,
                },
            },
        };
    }

}
