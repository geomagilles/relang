package com.relang;

import com.oracle.truffle.api.TruffleLanguage;

public final class ReLangContext {

    private final ReLang language;
    private final TruffleLanguage.Env env;
    private final java.util.Map<String, com.oracle.truffle.api.RootCallTarget> functionRegistry = new java.util.HashMap<>();

    public ReLangContext(ReLang language, TruffleLanguage.Env env) {
        this.language = language;
        this.env = env;
    }

    public java.util.Map<String, com.oracle.truffle.api.RootCallTarget> getFunctionRegistry() {
        return functionRegistry;
    }

    private static final com.oracle.truffle.api.TruffleLanguage.ContextReference<ReLangContext> REF = com.oracle.truffle.api.TruffleLanguage.ContextReference
            .create(ReLang.class);

    public static ReLangContext get(com.oracle.truffle.api.nodes.Node node) {
        return REF.get(node);
    }
}
