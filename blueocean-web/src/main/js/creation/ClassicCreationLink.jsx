import React from 'react';

import { i18nTranslator, buildClassicCreateJobUrl } from '../core-js';
import { Icon } from '../components/index-jdl';

const t = i18nTranslator('blueocean-dashboard');

export function ClassicCreationLink() {
    const newJobUrl = buildClassicCreateJobUrl();

    return (
        <a target="_blank" className={'classic-link'} href={newJobUrl}>
            <Icon icon="ActionExitToApp" size={24} style={{ marginRight: '8px' }} />

            <span>{t('creation.core.header.classic')}</span>
        </a>
    );
}
