package com.relang;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.nodes.FrameState;
import com.relang.nodes.ReLangNode;
import com.relang.nodes.ReLangReturnException;
import com.relang.nodes.ReLangSuspendException;
import com.relang.nodes.ResumableState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The root node for a ReLang function.
 * Handles:
 * <ul>
 *   <li>REWINDING (resume): Restores local variables and passes frame state to children</li>
 *   <li>UNWINDING (suspend): Catches ReLangSuspendException, captures locals, re-throws</li>
 * </ul>
 */
public class ReLangRootNode extends RootNode {

    @Child private ReLangNode bodyNode;

    /** Cached slot name to index mapping for O(1) lookup during restore. */
    @CompilationFinal private Map<String, Integer> slotNameToIndex;

    public ReLangRootNode(ReLang language, FrameDescriptor frameDescriptor, ReLangNode bodyNode) {
        super(language, frameDescriptor);
        this.bodyNode = bodyNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ReLangContext context = ReLangContext.get(this);
        ResumableState globalState = context.getResumptionState();
        FrameState myState = null;

        // REWINDING: Restore state if we are resuming
        if (globalState != null && !globalState.isEmpty()) {
            myState = globalState.popFrame();
            restoreLocals(frame, myState);
            context.setActiveFrameState(myState);
        }

        try {
            return bodyNode.executeGeneric(frame);
        } catch (ReLangReturnException e) {
            return e.getResult();
        } catch (ReLangSuspendException e) {
            // UNWINDING: Capture this frame's state and re-throw
            Map<String, Object> locals = captureLocals(frame);
            List<Integer> path = e.drainCurrentPath();
            e.getState().pushFrame(new FrameState(locals, path));
            throw e;
        } finally {
            if (myState != null) {
                context.setActiveFrameState(null);
            }
        }
    }

    private Map<String, Integer> getSlotNameToIndex() {
        if (slotNameToIndex == null) {
            FrameDescriptor descriptor = getFrameDescriptor();
            Map<String, Integer> mapping = new HashMap<>();
            for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
                Object slotName = descriptor.getSlotName(i);
                if (slotName != null) {
                    mapping.put(slotName.toString(), i);
                }
            }
            slotNameToIndex = mapping;
        }
        return slotNameToIndex;
    }

    private void restoreLocals(VirtualFrame frame, FrameState state) {
        Map<String, Integer> nameToIndex = getSlotNameToIndex();
        for (var entry : state.getLocals().entrySet()) {
            Integer slotIndex = nameToIndex.get(entry.getKey());
            if (slotIndex != null) {
                frame.setObject(slotIndex, entry.getValue());
            }
        }
    }

    private Map<String, Object> captureLocals(VirtualFrame frame) {
        Map<String, Object> locals = new HashMap<>();
        FrameDescriptor descriptor = getFrameDescriptor();
        for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
            Object val = frame.getValue(i);
            if (val != null) {
                Object slotName = descriptor.getSlotName(i);
                if (slotName != null) {
                    locals.put(slotName.toString(), val);
                }
            }
        }
        return locals;
    }
}
