// @flow

import React, {Component, PropTypes} from 'react';
import { describeArcAsPath } from '../SVG';

export const strokeWidth = 3.5; // px. Maybe we can fetch this from CSS at runtime in the future

export default class SvgSpinner extends Component {
    componentWillMount() {
        this.infiniteRotationRunning = false;
        this.setState({
            infiniteRotatePercentage: 0
        });
    }

    infiniteLoadingTimer = () => {
        let infiniteRotatePercentage = this.state.infiniteRotatePercentage;
        
        infiniteRotatePercentage += 1.5;

        if (infiniteRotatePercentage >= 360) {
            infiniteRotatePercentage = 0;
        }

        this.setState({
            infiniteRotatePercentage: infiniteRotatePercentage
        });

        requestAnimationFrame(this.infiniteLoadingTimer);
    }

    componentWillUnmount() {
        cancelAnimationFrame(this.infiniteLoadingTimer);
    }

    render() {

        const {result} = this.props;
        const radius = (this.props.radius || 12) - (0.5 * strokeWidth); // No "inside" stroking in SVG`

        let percentage = this.props.percentage;
        const groupClasses = ['progress-spinner', result];

        if (result === 'queued') {
            percentage = 0;
        }
        else if (result === 'not_built' || result === 'skipped') {
            percentage = 0;
        }
        else if (typeof percentage !== 'number' || isNaN(percentage) || percentage < 0) {
            percentage = 0;
        }
        else if (percentage === 100) {
            groupClasses.push('pc-over-100');
            percentage = 0;
        }
        else if (percentage > 100) {
            groupClasses.push('spin');
            percentage = 25;

            if (!this.infiniteRotationRunning) {
                requestAnimationFrame(this.infiniteLoadingTimer);

                this.infiniteRotationRunning = true;
            }
        }

        const rotate = percentage / 100 * 360;
        const d = describeArcAsPath(0, 0, radius, 0, rotate);

        const innerRadius = radius / 3;

        return (
            <g className={groupClasses.join(' ')} transform={`rotate(${this.state.infiniteRotatePercentage})`} >
                <circle cx="0" cy="0" r={radius} strokeWidth={strokeWidth}/>
                <circle className="inner" cx="0" cy="0" r={innerRadius} />
                { percentage ? <path className={result} fill="none" strokeWidth={strokeWidth} d={d}/> : null}
            </g>
        );
    }
}

SvgSpinner.propTypes = {
    percentage: PropTypes.number,
    radius: PropTypes.number,
    result: PropTypes.string,
};
