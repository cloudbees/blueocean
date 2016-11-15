import React from 'react';
import { assert } from 'chai';
import { shallow } from 'enzyme';

import PipelineRowItem from '../../main/js/components/PipelineRowItem.jsx';
import { PipelineRecord } from '../../main/js/components/records.jsx';

const hack = {
    MultiBranch: () => {},
    Pr: () => {},
    Activity: () => {},
};
/* eslint-disable quote-props */
const pipelineMulti = {
    'displayName': 'moreBeers',
    'name': 'morebeers',
    'fullName': 'beersland/morebeers',
    'fullDisplayName': 'beersland/moreBeers',
    'organization': 'jenkins',
    'weatherScore': 0,
    'branchNames': ['master'],
    'numberOfFailingBranches': 1,
    'numberOfFailingPullRequests': 0,
    'numberOfSuccessfulBranches': 0,
    'numberOfSuccessfulPullRequests': 0,
    'totalNumberOfBranches': 1,
    'totalNumberOfPullRequests': 0,
};
const pipelineMultiSuccess = {
    'displayName': 'moreBeersSuccess',
    'name': 'morebeersSuccess',
    'fullName': 'morebeersSuccess',
    'fullDisplayName': 'moreBeersSuccess',
    'organization': 'jenkins',
    'weatherScore': 0,
    'branchNames': ['master'],
    'numberOfFailingBranches': 0,
    'numberOfFailingPullRequests': 0,
    'numberOfSuccessfulBranches': 3,
    'numberOfSuccessfulPullRequests': 3,
    'totalNumberOfBranches': 3,
    'totalNumberOfPullRequests': 3,
};
const pipelineSimple = {
    'displayName': 'beers',
    'name': 'beers',
    'fullName': 'beers',
    'fullDisplayName': 'beers',
    'organization': 'jenkins',
    'weatherScore': 0,
};
/* eslint-enable quote-props */

describe('PipelineRecord', () => {
    it('create without error', () => {
        const pipelineRecord = new PipelineRecord(pipelineMultiSuccess);
        assert.isOk(pipelineRecord);
    });
});

describe('PipelineRowItem', () => {
    it('simple pipeline', () => {
        const wrapper = shallow(
            <PipelineRowItem
              hack={hack}
              pipeline={pipelineSimple}
              simple
            />
        );
        assert.equal(wrapper.find('tr').length, 1);

        const columns = wrapper.find('td');

        const nameCol = columns.at(0);
        const path = nameCol.find('Link').shallow().find('ExpandablePath');
        assert.equal(path.props().path, pipelineSimple.fullDisplayName);

        const weatherCol = columns.at(1);
        assert.equal(weatherCol.text(), '<WeatherIcon />');

        const multibranchCol = columns.at(2);
        assert.equal(multibranchCol.text(), ' - ');

        const pullRequestsCol = columns.at(3);
        assert.equal(pullRequestsCol.text(), ' - ');
    });

    describe('multiBranch', () => {
        it('with failing items', () => {
            const wrapper = shallow(
                <PipelineRowItem
                  hack={hack}
                  pipeline={pipelineMulti}
                />
            );
            assert.equal(wrapper.find('tr').length, 1);

            const columns = wrapper.find('td');

            const nameCol = columns.at(0);
            const path = nameCol.find('Link').shallow().find('ExpandablePath');
            assert.equal(path.props().path, pipelineMulti.fullDisplayName);

            const multibranchCol = columns.at(2).find('Link').shallow();
            assert.equal(multibranchCol.text(), '1 failing');

            const pullRequestsCol = columns.at(3);
            assert.equal(pullRequestsCol.text(), '');
        });

        it('with success', () => {
            const wrapper = shallow(
                <PipelineRowItem
                  hack={hack}
                  pipeline={pipelineMultiSuccess}
                />
            );
            assert.equal(wrapper.find('tr').length, 1);

            const columns = wrapper.find('td');

            const nameCol = columns.at(0);
            const path = nameCol.find('Link').shallow().find('ExpandablePath');
            assert.equal(path.props().path, pipelineMultiSuccess.fullDisplayName);

            const multibranchCol = columns.at(2).find('Link').shallow();
            assert.equal(multibranchCol.text(), '3 passing');

            const pullRequestsCol = columns.at(3).find('Link').shallow();
            assert.equal(pullRequestsCol.text(), '3 passing');
        });
    });
});
