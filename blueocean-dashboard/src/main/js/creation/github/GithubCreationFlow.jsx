/**
 * Created by cmeyers on 10/17/16.
 */
import React from 'react';
import MultiStepFlow from '../MultiStepFlow';
import VerticalStep from '../VerticalStep';

export default function GithubDefaultFlow() {
    return (
        <MultiStepFlow>
            <VerticalStep>
                <h1>Github Step</h1>
            </VerticalStep>
            <VerticalStep>
                <h1>Another Github Step</h1>
            </VerticalStep>
            <VerticalStep>
                <h1>Yet Another Github Step</h1>
            </VerticalStep>
            <VerticalStep>
                <h1>Completed</h1>
            </VerticalStep>
        </MultiStepFlow>
    );
}
