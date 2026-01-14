package com.relang.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.relang.ReLangTypeSystem;
import com.relang.ReLangTypeSystemGen;

@TypeSystemReference(ReLangTypeSystem.class)
@NodeInfo(language = "ReLang", description = "The abstract base node for all language nodes")
public abstract class ReLangNode extends Node {

    /**
     * The execute method that every node must implement.
     * Frame state for resumability is passed via ReLangContext.getActiveFrameState().
     */
    public abstract Object executeGeneric(VirtualFrame frame);

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return ReLangTypeSystemGen.expectLong(executeGeneric(frame));
    }
}
