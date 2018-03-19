import React, { Component, PropTypes } from 'react';
import { observer } from 'mobx-react';
import { TestSummary } from './TestSummary';
import TestSection from './TestSection';

/* eslint-disable max-len */

@observer
export default class TestResults extends Component {
    propTypes = {
        pipeline: PropTypes.object,
        run: PropTypes.object,
        t: PropTypes.func,
        locale: PropTypes.string,
        testService: PropTypes.object,
    };

    componentWillMount() {
        this._initPagers(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this._initPagers(nextProps);
    }

    _initPagers(props) {
        const pipeline = props.pipeline;
        const run = props.run;
        this.regressionsPager = this.props.testService.newRegressionsPager(pipeline, run);
        this.existingFailedPager = this.props.testService.newExistingFailedPager(pipeline, run);
        this.skippedPager = this.props.testService.newSkippedPager(pipeline, run);
        this.fixedPager = this.props.testService.newFixedPager(pipeline, run);
        this.passedPager = this.props.testService.newPassedPager(pipeline, run);
    }

    render() {
        const { t: translation, locale, run } = this.props;
        return (
            <div>
                <TestSummary
                    translate={translation}
                    passing={run.testSummary.passed}
                    fixed={run.testSummary.fixed}
                    failuresNew={run.testSummary.regressions}
                    failuresExisting={run.testSummary.existingFailed}
                    skipped={run.testSummary.skipped}
                />
                <TestSection
                    titleKey="rundetail.tests.results.errors.new.count"
                    pager={this.regressionsPager}
                    extraClasses="new-failure-block"
                    locale={locale}
                    t={translation}
                    total={run.testSummary.regressions}
                    testService={this.props.testService}
                />
                <TestSection
                    titleKey="rundetail.tests.results.errors.existing.count"
                    pager={this.existingFailedPager}
                    extraClasses="existing-failure-block"
                    locale={locale}
                    t={translation}
                    total={run.testSummary.existingFailed}
                    testService={this.props.testService}
                />
                <TestSection
                    titleKey="rundetail.tests.results.fixed"
                    pager={this.fixedPager}
                    extraClasses="fixed-block"
                    locale={locale}
                    t={translation}
                    total={run.testSummary.fixed}
                    testService={this.props.testService}
                />
                <TestSection
                    titleKey="rundetail.tests.results.skipped.count"
                    pager={this.skippedPager}
                    extraClasses="skipped-block"
                    locale={locale}
                    t={translation}
                    total={run.testSummary.skipped}
                    testService={this.props.testService}
                />
                <TestSection
                    titleKey="rundetail.tests.results.passed.count"
                    pager={this.passedPager}
                    extraClasses=""
                    locale={locale}
                    t={translation}
                    total={run.testSummary.passed}
                    testService={this.props.testService}
                />
            </div>
        );
    }
}
