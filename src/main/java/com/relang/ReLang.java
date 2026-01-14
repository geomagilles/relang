package com.relang;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.nodes.ReLangNode;
import com.relang.nodes.LongLiteralNode;

import java.util.Map;

@TruffleLanguage.Registration(id = "relang", name = "ReLang", defaultMimeType = "application/x-relang", characterMimeTypes = "application/x-relang")
public final class ReLang extends TruffleLanguage<ReLangContext> {

    @Override
    protected ReLangContext createContext(Env env) {
        return new ReLangContext(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Map<String, RootCallTarget> functions = com.relang.parser.ReLangTruffleParser.parse(this, request.getSource());
        ReLangContext context = getCurrentContext(ReLang.class);
        context.getFunctionRegistry().putAll(functions);
        return functions.get("main");
    }
}
