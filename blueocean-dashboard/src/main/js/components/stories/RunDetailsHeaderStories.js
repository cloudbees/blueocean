/* eslint-disable */
import React from 'react';
import {storiesOf, action} from '@kadira/storybook';
import {
    ModalView,
    ModalBody,
    ModalHeader,
    PageTabs,
    Progress,
    TabLink,
} from '@jenkins-cd/design-language';
import WithContext from '@jenkins-cd/design-language/dist/js/stories/WithContext';
import {RunDetailsHeader} from '../RunDetailsHeader';
import {RunRecord} from '../records';

import {testData} from './data/changesData';

const runJSON = JSON.stringify(testData.run);
const temp = JSON.parse(runJSON);
// temp.changeSet = new ChangeSetRecord(temp.changeSet);
const run = new RunRecord(temp);

const strings = {
    "common.date.duration.format": "m[ minutes] s[ seconds]",
    "common.date.duration.hint.format": "M [month], d [days], h[h], m[m], s[s]",
    "common.date.readable.long": "MMM DD YYYY h:mma Z",
    "common.date.readable.short": "MMM DD h:mma Z",
    "rundetail.header.branch": "Branch",
    "rundetail.header.changes.names": "Changes by {0}",
    "rundetail.header.changes.none": "No changes",
    "rundetail.header.commit": "Commit",
};

const t = (key) => strings[key] || key;

const ctx = {
    config: {
        getServerBrowserTimeSkewMillis: () => {
            return 0;
        }
    }
};

RunDetailsHeader.logger = {
    debug: (...rest) => {
        console.debug(...rest);
    }
};

RunDetailsHeader.timeManager = {
    harmonizeTimes: obj => obj
};

storiesOf('Run Details Header', module)
    .add('Basic', basic)
;

function basic() {

    const topNavLinks = [
        <a href="#" className="selected">Pipeline</a>,
        <a href="#">Changes</a>,
        <a href="#">Tests</a>,
        <a href="#">Artifacts</a>,
    ];

    return (
        <WithContext context={ctx}>
            <RunDetailsHeader
                locale="en"
                t={t}
                pipeline={testData.pipeline}
                data={run}
                onOrganizationClick={ action('button-click')}
                onNameClick={ action('button-click')}
                onAuthorsClick={ action('button-click')}
                topNavLinks={topNavLinks}/>
        </WithContext>
    );
}
