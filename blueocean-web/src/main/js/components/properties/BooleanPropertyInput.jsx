import React from 'react';
import { Checkbox } from '../../components/index-jdl';
import { getArg, setArg } from '../../services/PipelineMetadataService';

export default class BooleanPropertyInput extends React.Component {
    render() {
        return (
            <div>
                <Checkbox checked={getArg(this.props.step, this.props.propName).value}
                    onToggle={checked => { setArg(this.props.step, this.props.propName, checked); this.props.onChange(this.props.step); }}
                    label={this.props.type.capitalizedName + (this.props.type.isRequired ? '*' : '')} />
            </div>
        );
    }
}

BooleanPropertyInput.propTypes = {
    propName: React.PropTypes.string,
    step: React.PropTypes.any,
    onChange: React.PropTypes.func,
    type: React.PropTypes.any,
};

BooleanPropertyInput.dataTypes = [ 'boolean', 'java.lang.Boolean' ];
