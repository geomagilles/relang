package com.relang;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.nodes.ReLangSuspendException;
import com.relang.nodes.ResumableState;
import com.relang.nodes.SuspendedResult;

import java.util.Map;

@TruffleLanguage.Registration(id = "relang", name = "ReLang", defaultMimeType = "application/x-relang", characterMimeTypes = "application/x-relang")
public final class ReLang extends TruffleLanguage<ReLangContext> {

    @Override
    protected ReLangContext createContext(Env env) {
        return new ReLangContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        String sourceCode = request.getSource().getCharacters().toString();
        String sourceHash = ResumableState.computeSourceHash(sourceCode);
        
        Map<String, RootCallTarget> functions = com.relang.parser.ReLangTruffleParser.parse(this, request.getSource());
        ReLangContext context = ReLangContext.get(null);
        context.getFunctionRegistry().putAll(functions);
        context.setCurrentSourceHash(sourceHash);
        
        // Verify source hash if resuming - fail fast if code changed
        ResumableState resumeState = context.getResumptionState();
        if (resumeState != null && resumeState.getSourceHash() != null) {
            resumeState.verifySourceHash(sourceCode);
        }

        // Get the entry point (main body or main function)
        RootCallTarget entryPoint;
        if (functions.containsKey("")) {
            entryPoint = functions.get("");
        } else {
            entryPoint = functions.get("main");
        }

        // Wrap in a top-level node that catches ReLangSuspendException
        return new TopLevelRootNode(this, entryPoint).getCallTarget();
    }

    /**
     * Top-level wrapper that catches ReLangSuspendException and converts it to SuspendedResult.
     */
    private static class TopLevelRootNode extends RootNode {
        private final RootCallTarget entryPoint;

        TopLevelRootNode(ReLang language, RootCallTarget entryPoint) {
            super(language, FrameDescriptor.newBuilder().build());
            this.entryPoint = entryPoint;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            ReLangContext context = ReLangContext.get(this);
            try {
                return entryPoint.call();
            } catch (ReLangSuspendException e) {
                // Set the source hash on the state for validation on resume
                e.getState().setSourceHash(context.getCurrentSourceHash());
                
                // Convert exception to SuspendedResult for the host
                return context.getEnv().asGuestValue(new SuspendedResult(e.getState()));
            }
        }
    }
}
