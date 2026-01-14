package com.relang;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.nodes.ReLangNode;
import com.relang.nodes.ReLangReturnException;
import com.relang.nodes.ReLangSuspendException;
import com.relang.nodes.ResumableState;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * The root node for a ReLang function.
 * Handles:
 * - REWINDING (resume): Restores local variables and passes frame state to children
 * - UNWINDING (suspend): Catches ReLangSuspendException, captures locals, re-throws
 */
public class ReLangRootNode extends RootNode {

    @Child
    private ReLangNode bodyNode;

    public ReLangRootNode(ReLang language, FrameDescriptor frameDescriptor, ReLangNode bodyNode) {
        super(language, frameDescriptor);
        this.bodyNode = bodyNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ReLangContext context = ReLangContext.get(this);
        ResumableState globalState = context.getResumptionState();
        ResumableState.FrameState myState = null;

        // REWINDING: Restore state if we are resuming
        if (globalState != null && !globalState.isEmpty()) {
            myState = globalState.popFrame();
            restoreLocals(frame, myState);
            context.setActiveFrameState(myState);
        }

        try {
            Object result = bodyNode.executeGeneric(frame);
            return result;
        } catch (ReLangReturnException e) {
            return e.getResult();
        } catch (ReLangSuspendException e) {
            // UNWINDING: Capture this frame's state and re-throw
            Map<String, Object> locals = captureLocals(frame);
            Stack<Integer> path = e.drainCurrentPath();  // Get path accumulated by BlockNodes
            ResumableState.FrameState frameState = new ResumableState.FrameState(locals, path);
            e.getState().pushFrame(frameState);
            throw e;
        } finally {
            if (myState != null) {
                context.setActiveFrameState(null);
            }
        }
    }

    private void restoreLocals(VirtualFrame frame, ResumableState.FrameState state) {
        FrameDescriptor descriptor = getFrameDescriptor();
        for (Map.Entry<String, Object> entry : state.getLocals().entrySet()) {
            for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
                Object slotName = descriptor.getSlotName(i);
                if (entry.getKey().equals(slotName)) {
                    frame.setObject(i, entry.getValue());
                    break;
                }
            }
        }
    }

    private Map<String, Object> captureLocals(VirtualFrame frame) {
        Map<String, Object> locals = new HashMap<>();
        FrameDescriptor descriptor = getFrameDescriptor();
        for (int i = 0; i < descriptor.getNumberOfSlots(); i++) {
            Object val = frame.getValue(i);
            if (val != null) {
                locals.put(descriptor.getSlotName(i).toString(), val);
            }
        }
        return locals;
    }
}
