package com.relang;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.relang.nodes.AwaitableTable;
import com.relang.nodes.FrameState;
import com.relang.nodes.ResumableState;
import com.relang.nodes.SuspendedResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-context state for ReLang execution.
 * Manages function registry, resumption state, and source code hash.
 */
public final class ReLangContext {

    private static final TruffleLanguage.ContextReference<ReLangContext> REF =
            TruffleLanguage.ContextReference.create(ReLang.class);
    private final TruffleLanguage.Env env;
    private final Map<String, FunctionDescriptor> functionRegistry = new HashMap<>();
    private final AwaitableTable awaitableTable = new AwaitableTable();
    // The frame state for the currently executing function during resume
    private FrameState activeFrameState;
    // SHA-256 hash of the current source code, used for state validation
    private String currentSourceHash;

    public ReLangContext(ReLang language, TruffleLanguage.Env env) {
        this.env = env;
    }

    public static ReLangContext get(Node node) {
        return REF.get(node);
    }

    public String getCurrentSourceHash() {
        return currentSourceHash;
    }

    public void setCurrentSourceHash(String hash) {
        this.currentSourceHash = hash;
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    public Map<String, FunctionDescriptor> getFunctionRegistry() {
        return functionRegistry;
    }

    /**
     * Gets resumption state from polyglot bindings if available.
     * Host can set: context.getPolyglotBindings().putMember("resumeState", suspendedResult);
     */
    public ResumableState getResumptionState() {
        try {
            Object bindings = env.getPolyglotBindings();
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();

            if (interop.isMemberReadable(bindings, "resumeState")) {
                Object value = interop.readMember(bindings, "resumeState");
                if (env.isHostObject(value)) {
                    Object hostObj = env.asHostObject(value);
                    if (hostObj instanceof SuspendedResult) {
                        return ((SuspendedResult) hostObj).state();
                    }
                }
            }
        } catch (SecurityException e) {
            // Polyglot bindings not accessible - not resuming
        } catch (Exception e) {
            // Ignore other errors reading bindings
        }
        return null;
    }

    public FrameState getActiveFrameState() {
        return activeFrameState;
    }

    public void setActiveFrameState(FrameState state) {
        this.activeFrameState = state;
    }

    public AwaitableTable getAwaitableTable() {
        return awaitableTable;
    }

    /**
     * Restore awaitable table entries from a resumed state.
     * Merges the given table's entries into this context's table.
     */
    public void restoreAwaitableTable(AwaitableTable table) {
        this.awaitableTable.getAll().putAll(table.getAll());
    }
}
