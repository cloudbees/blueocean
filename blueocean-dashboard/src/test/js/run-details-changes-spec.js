import { assert } from 'chai';
import React from 'react';
import sd from 'skin-deep';
import { i18nTranslator } from '@jenkins-cd/blueocean-core-js';

import { latestRuns } from './data/runs/latestRuns';
import RunDetailsChanges from '../../main/js/components/RunDetailsChanges';

const t = i18nTranslator('blueocean-dashboard');

import { mockExtensionsForI18n } from './mock-extensions-i18n';
mockExtensionsForI18n();

describe('RunDetailsChanges', () => {
    beforeAll(() => mockExtensionsForI18n());

    let component;
    let tree;
    let output;

    describe('empty runs / bad data', () => {
        beforeAll(() => {
            component = (
                <RunDetailsChanges t={t} />
            );
            tree = sd.shallowRender(component);
            output = tree.getRenderOutput();
        });

        it('renders nothing', () => {
            assert.isNull(output);
        });
    });

    describe('empty changeSet', () => {
        beforeAll(() => {
            component = (
                <RunDetailsChanges
                  t={t}
                  result={{ changeSet: [] }}
                />
            );
            tree = sd.shallowRender(component);
            output = tree.getRenderOutput();
        });

        it('renders NoChangesPlaceholder', () => {
            assert.equal(output.type.name, 'NoChangesPlaceholder');
        });
    });

    describe('valid changeSet', () => {
        beforeAll(() => {
            const runs = latestRuns.map(run => (run.latestRun));
            component = (
                <RunDetailsChanges
                  t={t}
                  result={runs[0]}
                />
            );
            tree = sd.shallowRender(component);
            output = tree.getRenderOutput();
        });

        it('renders a Table with expected data', () => {
            assert.equal(output.type.name, 'Table');
            assert.equal(tree.everySubTree('tr').length, 2);

            const cols = tree.subTree('tr').everySubTree('td');
            assert.equal(cols[0].text(), '<CommitLink />');
            assert.equal(cols[1].text(), 'tscherler');
            assert.equal(cols[2].text(), 'Update Jenkinsfile');
            assert.equal(cols[3].text(), '<ReadableDate />');
        });
    });
});
