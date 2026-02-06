package com.relang;

import com.oracle.truffle.api.RootCallTarget;
import java.util.List;

/**
 * Metadata about a declared function, including its call target and parameter names.
 */
public record FunctionDescriptor(
    RootCallTarget callTarget,
    List<String> parameterNames,
    int requiredParams
) {
}
