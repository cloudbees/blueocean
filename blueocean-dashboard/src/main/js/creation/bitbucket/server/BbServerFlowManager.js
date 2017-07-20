import React from 'react';
import { i18nTranslator } from '@jenkins-cd/blueocean-core-js';

import waitAtLeast from '../../flow2/waitAtLeast';
import BbCloudFlowManager from '../cloud/BbCloudFlowManager';
import BbLoadingStep from '../steps/BbLoadingStep';

import BbChooseServerStep from './steps/BbChooseServerStep';
import BbServerManager from './BbServerManager';
import STATE from './BbServerCreationState';


const translate = i18nTranslator('blueocean-dashboard');
const MIN_DELAY = 500;


export default class BbServerFlowManager extends BbCloudFlowManager {

    selectedServer = null;

    constructor(creationApi, credentialsApi, serverApi) {
        super(creationApi, credentialsApi);
        this.serverManager = new BbServerManager(serverApi);
    }

    translate(key, opts) {
        return translate(key, opts);
    }

    getStates() {
        return STATE.values();
    }

    getState() {
        return STATE;
    }

    getInitialStep() {
        return {
            stateId: STATE.PENDING_LOADING_SERVERS,
            stepElement: <BbLoadingStep />,
        };
    }

    onInitialized() {
        this._loadServerList();
        this.setPlaceholders(translate('creation.core.status.completed'));
    }

    getApiUrl() {
        return this.selectedServer ? this.selectedServer.apiUrl : null;
    }

    _getCredentialsStepAfterStateId() {
        return STATE.STEP_CHOOSE_SERVER;
    }

    _getOrganizationsStepAfterStateId() {
        return this.isStateAdded(STATE.STEP_CREDENTIAL) ?
            STATE.STEP_CREDENTIAL : STATE.STEP_CHOOSE_SERVER;
    }

    _loadServerList() {
        return this.serverManager.listServers()
            .then(waitAtLeast(MIN_DELAY))
            .then(success => this._loadServerListComplete(success));
    }

    _loadServerListComplete() {
        this.renderStep({
            stateId: STATE.STEP_CHOOSE_SERVER,
            stepElement: <BbChooseServerStep />,
        });
    }

    selectServer(server) {
        this.selectedServer = server;

        this.findExistingCredential();
        this.renderStep({
            stateId: STATE.PENDING_LOADING_CREDS,
            stepElement: <BbLoadingStep />,
            afterStateId: STATE.STEP_CHOOSE_SERVER,
        });
    }
}
