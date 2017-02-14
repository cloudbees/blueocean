import React, { PropTypes } from 'react';
import VerticalStep from './VerticalStep';
import Status from './FlowStepStatus';

/**
 * Visual/logic component that defines an individual step of a multi-step workflow.
 * Intended to be used within a MultiStepFlow component.
 * Hides all content except for the title until the step becomes active.
 */
export default function FlowStep(props) {
    const percentage = props.loading ? 101 : props.percentage;
    const status = props.error ? Status.ERROR : props.status;

    return (
        <VerticalStep
          className={props.className}
          status={status}
          percentage={percentage}
          isLastStep={props.isLastStep}
        >
            <h1>{props.title}</h1>
            {
                props.status !== Status.INCOMPLETE &&
                <fieldset disabled={props.disabled}>
                    {props.children}
                </fieldset>
            }
        </VerticalStep>
    );
}

FlowStep.propTypes = {
    children: PropTypes.node,
    className: PropTypes.string,
    title: PropTypes.string,
    status: PropTypes.string,
    percentage: PropTypes.number,
    disabled: PropTypes.bool,
    loading: PropTypes.bool,
    error: PropTypes.bool,
    isLastStep: PropTypes.bool,
};

FlowStep.defaultProps = {
    className: '',
};
