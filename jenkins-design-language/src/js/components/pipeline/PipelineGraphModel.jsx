// @flow

import type { Result } from '../status/StatusIndicator';

// Dimensions used for layout, px
export const defaultLayout = {
    nodeSpacingH: 120,
    nodeSpacingV: 70,
    nodeRadius: 12,
    terminalRadius: 7,
    curveRadius: 12,
    connectorStrokeWidth: 3.5,
    labelOffsetV: 20,
    smallLabelOffsetV: 15,
    ypStart: 55,
};

// Typedefs

export type StageInfo = {
    name: string,
    title: string,
    state: Result,
    completePercent: number,
    id: number,
    children: Array<StageInfo>
};

export type StageNodeInfo = {
    // -- Shared with PlaceholderNodeInfo
    key: string,
    x: number,
    y: number,
    id: number,
    name: string,

    // -- Marker
    isPlaceholder: false,

    // -- Unique
    stage: StageInfo
};

export type PlaceholderNodeInfo = {
    // -- Shared with StageNodeInfo
    key: string,
    x: number,
    y: number,
    id: number,
    name: string,

    // -- Marker
    isPlaceholder: true,

    // -- Unique
    type: 'start' | 'end'
}

// TODO: Attempt to extract a "common" node type with intersection operator to remove duplication

export type NodeInfo = StageNodeInfo | PlaceholderNodeInfo;

export type NodeColumn = {
    topStage?: StageInfo,
    nodes: Array<NodeInfo>,
}

export type CompositeConnection = {
    sourceNodes: Array<NodeInfo>,
    destinationNodes: Array<NodeInfo>,
    skippedNodes: Array<NodeInfo>
};

export type LabelInfo = {
    x: number,
    y: number,
    text: string,
    key: string,
    stage?: StageInfo,
    node: NodeInfo
};

export type LayoutInfo = typeof defaultLayout;
