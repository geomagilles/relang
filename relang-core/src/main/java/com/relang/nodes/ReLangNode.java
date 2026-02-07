package com.relang.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.relang.ReLangTypeSystem;
import com.relang.ReLangTypeSystemGen;

@GenerateWrapper
@TypeSystemReference(ReLangTypeSystem.class)
@NodeInfo(language = "ReLang", description = "The abstract base node for all language nodes")
public abstract class ReLangNode extends Node implements InstrumentableNode {

    @CompilationFinal private SourceSection sourceSection;

    /**
     * Unit value returned by statements that don't produce a meaningful result.
     * Used by control flow nodes (if without else, while, empty blocks).
     */
    public static final Object UNIT = ReLangUnit.SINGLETON;

    /**
     * Assign source location to this node. Called by the parser to enable
     * LSP hover and other source-position-dependent tooling.
     */
    public void assignSourceSection(SourceSection section) {
        this.sourceSection = section;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override
    public boolean isInstrumentable() {
        return sourceSection != null;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new ReLangNodeWrapper(this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return false;
    }

    /**
     * The execute method that every node must implement.
     * Frame state for resumability is passed via ReLangContext.getActiveFrameState().
     */
    public abstract Object executeGeneric(VirtualFrame frame);

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return ReLangTypeSystemGen.expectLong(executeGeneric(frame));
    }

    /**
     * Evaluate a value as a boolean condition.
     * Only Bool values are accepted — no implicit truthiness for Int or other types.
     *
     * @param value the result of evaluating a condition expression
     * @return the boolean value
     * @throws ReLangTypeError if the value is not Bool
     */
    protected boolean evaluateAsBoolean(Object value) {
        return switch (value) {
            case Boolean b -> b;
            default -> throw new ReLangTypeError(this, RuntimeDiagnostics.conditionMustBeBool(value));
        };
    }
}
