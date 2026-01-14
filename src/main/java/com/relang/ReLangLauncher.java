package com.relang;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class ReLangLauncher {
    public static void main(String[] args) {
        try (Context context = Context.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {
            Value result = context.eval("relang", "fn add(a, b) { return a + b; } add(10, 20);");
            System.out.println("Result: " + result.asInt());
        }
    }
}
