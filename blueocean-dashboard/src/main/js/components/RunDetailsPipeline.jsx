import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import Extensions from '@jenkins-cd/js-extensions';
import LogConsole from './LogConsole';
import * as sse from '@jenkins-cd/sse-gateway';
import { EmptyStateView } from '@jenkins-cd/design-language';

import LogToolbar from './LogToolbar';
import Steps from './Steps';
import {
    steps as stepsSelector,
    logs as logSelector,
    node as nodeSelector,
    nodes as nodesSelector,
    actions,
    connect,
    createSelector,
} from '../redux';

import { calculateStepsBaseUrl, calculateRunLogURLObject, calculateNodeBaseUrl } from '../util/UrlUtils';
import { calculateNode } from '../util/KaraokeHelper';


const { string, object, any, func } = PropTypes;

export class RunDetailsPipeline extends Component {
    constructor(props) {
        super(props);
        // we do not want to follow any builds that are finished
        this.state = { followAlong: props && props.result && props.result.state !== 'FINISHED' };
        this.listener = {};
        this._handleKeys = this._handleKeys.bind(this);
        this.onScrollHandler = this.onScrollHandler.bind(this);
    }

    componentWillMount() {
        const { fetchNodes, fetchLog, result, fetchSteps } = this.props;

        this.mergedConfig = this.generateConfig(this.props);

        // It should really be using capability using /rest/classes API
        const supportsNode = result && result._class === 'io.jenkins.blueocean.rest.impl.pipeline.PipelineRunImpl';
        if (supportsNode) {
            fetchNodes(this.mergedConfig);
        } else {
            // console.log('fetch the log directly')
            const logGeneral = calculateRunLogURLObject(this.mergedConfig);
            fetchLog({ ...logGeneral });
        }

        // Listen for pipeline flow node events.
        // We filter them only for steps and the end event all other we let pass
        const onSseEvent = (event) => {
            const jenkinsEvent = event.jenkins_event;
            // we are using try/catch to throw an early out error
            try {
                if (event.pipeline_run_id !== this.props.result.id) {
                    // console.log('early out');
                    throw new Error('exit');
                }
                // we turn on refetch so we always fetch a new Node result
                const refetch = true;
                switch (jenkinsEvent) {
                case 'pipeline_step':
                    {
                        // we are not using an early out for the events since we want to refresh the node if we finished
                        if (this.state.followAlong) { // if we do it means we want karaoke
                            // if the step_stage_id has changed we need to change the focus
                            if (event.pipeline_step_stage_id !== this.mergedConfig.node) {
                                // console.log('nodes fetching via sse triggered');
                                delete this.mergedConfig.node;
                                fetchNodes({ ...this.mergedConfig, refetch });
                            } else {
                                // console.log('only steps fetching via sse triggered');
                                fetchSteps({ ...this.mergedConfig, refetch });
                            }
                        }
                        break;
                    }
                case 'pipeline_end':
                    {
                        // we always want to refresh if the run has finished
                        fetchNodes({ ...this.mergedConfig, refetch });
                        break;
                    }
                default:
                    {
                        // //console.log(event);
                    }
                }
            } catch (e) {
                // we only ignore the exit error
                if (e.message !== 'exit') {
                    throw e;
                }
            }
        };

        this.listener.sse = sse.subscribe('pipeline', onSseEvent);
    }

    componentDidMount() {
        // determine scroll area
        const domNode = ReactDOM.findDOMNode(this.refs.scrollArea);
        // add both listemer, one to the scroll area and another to the whole document
        domNode.addEventListener('wheel', this.onScrollHandler, false);
        document.addEventListener('keydown', this._handleKeys, false);
    }

    componentWillReceiveProps(nextProps) {
        const followAlong = this.state.followAlong;
        this.mergedConfig = this.generateConfig({ ...nextProps, followAlong });

        // we do not want any timeouts if we are not doing karaoke
        if (!this.state.followAlong && this.timeout) {
            clearTimeout(this.timeout);
        }
        // calculate if we need to trigger any actions to get into the right state (is plain js for testing reasons)
        const nodeAction = calculateNode(this.props, nextProps, this.mergedConfig);
        if (nodeAction && nodeAction.action) {
            // use updated config
            this.mergedConfig = nodeAction.config;
            // we may need to stop following
            if (this.state.followAlong !== nodeAction.state.followAlong) {
                this.setState({ followAlong: nodeAction.state.followAlong });
            }
            // if we have actions we fire them
            this.props[nodeAction.action](this.mergedConfig);
        }
        // if we only interested in logs (in case of e.g. freestyle)
        const { logs, fetchLog } = nextProps;
        if (logs !== this.props.logs) {
            const logGeneral = calculateRunLogURLObject(this.mergedConfig);
            const log = logs ? logs[logGeneral.url] : null;
            if (log && log !== null) {
                // we may have a streaming log
                const newStart = log.newStart;
                if (Number(newStart) > 0) {
                    // in case we doing karaoke we want to see more logs
                    if (this.state.followAlong) {
                        // kill current  timeout if any
                        clearTimeout(this.timeout);
                        // we need to get mpre input from the log stream
                        this.timeout = setTimeout(() => fetchLog({ ...logGeneral, newStart }), 1000);
                    }
                }
            }
        }
    }

    componentWillUnmount() {
        const domNode = ReactDOM.findDOMNode(this.refs.scrollArea);
        domNode.removeEventListener('wheel', this._onScrollHandler);
        document.removeEventListener('keydown', this._handleKeys);
        if (this.listener.sse) {
            sse.unsubscribe(this.listener.sse);
            delete this.listener.sse;
        }
        this.props.cleanNodePointer();
        clearTimeout(this.timeout);
    }

    // need to register handler to step out of karaoke mode
    // we bail out on scroll up
    onScrollHandler(elem) {
        if (elem.deltaY < 0 && this.state.followAlong) {
            this.setState({ followAlong: false });
        }
    }
    // we bail out on arrow_up key
    _handleKeys(event) {
        if (event.keyCode === 38 && this.state.followAlong) {
            this.setState({ followAlong: false });
        }
    }

    generateConfig(props) {
        const {
            config = {},
        } = this.context;
        const followAlong = this.state.followAlong;
        const {
            isMultiBranch,
            params: { pipeline: name, branch, runId, node: nodeParam },
        } = props;
        // we would use default properties however the node can be null so no default properties will be triggered
        let { nodeReducer } = props;
        if (!nodeReducer) {
            nodeReducer = { id: null, displayName: 'Steps' };
        }
        // if we have a node param we do not want the calculation of the focused node
        const node = nodeParam || nodeReducer.id;

        const mergedConfig = { ...config, name, branch, runId, isMultiBranch, node, nodeReducer, followAlong };
        return mergedConfig;
    }

    render() {
        const {
            location,
            router,
        } = this.context;

        const {
            params: {
                pipeline: name, branch, runId,
            },
            isMultiBranch, steps, nodes, logs, result: resultMeta,
        } = this.props;

        const {
            result,
            state,
        } = resultMeta;
        const resultRun = result === 'UNKNOWN' || !result ? state : result;
        const followAlong = this.state.followAlong;
        // in certain cases we want that the log component will scroll to the end of a log
        const scrollToBottom =
                resultRun.toLowerCase() === 'failure'
                || (resultRun.toLowerCase() === 'running' && followAlong)
            ;

        const nodeKey = calculateNodeBaseUrl(this.mergedConfig);
        const key = calculateStepsBaseUrl(this.mergedConfig);
        const logGeneral = calculateRunLogURLObject(this.mergedConfig);
        const log = logs ? logs[logGeneral.url] : null;
        let title = this.mergedConfig.nodeReducer.displayName;
        if (log) {
            title = 'Logs';
        } else if (this.mergedConfig.nodeReducer.id !== null && title) {
            title = `Steps - ${title}`;
        }
        const currentSteps = steps ? steps[key] : null;
        // here we decide what to do next if somebody clicks on a flowNode
        const afterClick = (id) => {
            // get some information about the node the user clicked
            const nodeInfo = nodes[nodeKey].model.filter((item) => item.id === id)[0];
            const pathname = location.pathname;
            let newPath;
            // if path ends with pipeline we simply use it
            if (pathname.endsWith('pipeline/')) {
                newPath = pathname;
            } else if (pathname.endsWith('pipeline')) {
                newPath = `${pathname}/`;
            } else {
                // remove last bits
                const pathArray = pathname.split('/');
                pathArray.pop();
                if (pathname.endsWith('/')) {
                    pathArray.pop();
                }
                pathArray.shift();
                newPath = `${pathArray.join('/')}/`;
            }
            // we only want to redirect to the node if the node is finished
            if (nodeInfo.state === 'FINISHED') {
                newPath = `${newPath}${id}`;
            }
            // see whether we need to update the state
            if (nodeInfo.state === 'FINISHED' && followAlong) {
                this.setState({ followAlong: false });
            }
            if (nodeInfo.state !== 'FINISHED' && !followAlong) {
                this.setState({ followAlong: true });
            }
            router.push(newPath);
        };
        const noSteps = !log && currentSteps && currentSteps.model && currentSteps.model.length === 0;
        const shouldShowLogHeader = log !== null || !noSteps;
        return (
            <div>
            <Extensions.Renderer
              extensionPoint="jenkins.pipeline.run.details"
              currentRun={this.props.result}
            />
            <div ref="scrollArea">
                { nodes && nodes[nodeKey] && <Extensions.Renderer
                  extensionPoint="jenkins.pipeline.run.result"
                  selectedStage={this.mergedConfig.nodeReducer}
                  callback={afterClick}
                  nodes={nodes[nodeKey].model}
                  pipelineName={name}
                  branchName={isMultiBranch ? branch : undefined}
                  runId={runId}
                />
                }
                { shouldShowLogHeader &&
                    <LogToolbar
                      fileName={logGeneral.fileName}
                      url={logGeneral.url}
                      title={title}
                    />
                }
                { currentSteps && <Steps
                  nodeInformation={currentSteps}
                  followAlong={followAlong}
                  router={router}
                  {...this.props}
                />
                }
                { noSteps && <EmptyStateView tightSpacing>
                    <p>There are no steps.</p>
                </EmptyStateView>
                }

                { log && <LogConsole key={logGeneral.url} logArray={log.logArray} scrollToBottom={scrollToBottom} /> }
            </div>
            </div>
        );
    }
}

RunDetailsPipeline.propTypes = {
    pipeline: object,
    isMultiBranch: any,
    params: object,
    result: object,
    fileName: string,
    url: string,
    fetchLog: func,
    fetchNodes: func,
    setNode: func,
    fetchSteps: func,
    cleanNodePointer: func,
    logs: object,
    steps: object,
    nodes: object,
    nodeReducer: object,
};

RunDetailsPipeline.contextTypes = {
    config: object.isRequired,
    params: object,
    pipeline: object,
    router: object.isRequired, // From react-router
    location: object.isRequired, // From react-router
};

const selectors = createSelector(
    [stepsSelector, logSelector, nodeSelector, nodesSelector],
    (steps, logs, nodeReducer, nodes) => ({ steps, logs, nodeReducer, nodes }));

export default connect(selectors, actions)(RunDetailsPipeline);
