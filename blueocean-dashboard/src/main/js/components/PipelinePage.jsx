import React, { Component, PropTypes } from 'react';
import { Link } from 'react-router';
import {
    Page,
    PageHeader,
    Title,
    PageTabs,
    TabLink,
    WeatherIcon,
    Favorite,
} from '@jenkins-cd/design-language';

const { object } = PropTypes;

export default class PipelinePage extends Component {
    render() {
        const { pipeline } = this.context;
        const { organization, name } = pipeline || {};
        const orgUrl = `/organizations/${organization}`;
        const activityUrl = `${orgUrl}/${name}/activity`;

        if (!pipeline) {
            return null; // Loading...
        }

        const baseUrl = `/organizations/${pipeline.organization}/${pipeline.name}`;

        return (
            <Page>
                <PageHeader>
                    <Title>
                        <WeatherIcon score={pipeline.weatherScore} size="large" />
                        <h1>
                            <Link to={orgUrl}>{organization}</Link>
                            <span> / </span>
                            <Link to={activityUrl}>{name}</Link>
                        </h1>
                        <Favorite darkTheme />
                    </Title>
                    <PageTabs base={baseUrl}>
                        <TabLink to="/activity">Activity</TabLink>
                        <TabLink to="/branches">Branches</TabLink>
                        <TabLink to="/pr">Pull Requests</TabLink>
                    </PageTabs>
                </PageHeader>
                {React.cloneElement(this.props.children, { pipeline })}
            </Page>
        );
    }
}

PipelinePage.propTypes = {
    children: object,
};

PipelinePage.contextTypes = {
    location: object,
    pipeline: object,
};
