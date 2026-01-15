package com.relang;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.relang.nodes.ResumableState;
import com.relang.nodes.SuspendedResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-context state for ReLang execution.
 * Manages function registry, resumption state, and source code hash.
 */
public final class ReLangContext {

    private final TruffleLanguage.Env env;
    private final Map<String, RootCallTarget> functionRegistry = new HashMap<>();

    // The frame state for the currently executing function during resume
    private ResumableState.FrameState activeFrameState;
    
    // SHA-256 hash of the current source code, used for state validation
    private String currentSourceHash;

    public ReLangContext(ReLang language, TruffleLanguage.Env env) {
        this.env = env;
    }
    
    public void setCurrentSourceHash(String hash) {
        this.currentSourceHash = hash;
    }
    
    public String getCurrentSourceHash() {
        return currentSourceHash;
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    public Map<String, RootCallTarget> getFunctionRegistry() {
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
                        return ((SuspendedResult) hostObj).getState();
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

    public void setActiveFrameState(ResumableState.FrameState state) {
        this.activeFrameState = state;
    }

    public ResumableState.FrameState getActiveFrameState() {
        return activeFrameState;
    }

    private static final TruffleLanguage.ContextReference<ReLangContext> REF = 
            TruffleLanguage.ContextReference.create(ReLang.class);

    public static ReLangContext get(Node node) {
        return REF.get(node);
    }
}
