package com.relang.launcher;

/**
 * Exit codes for the ReLang launcher following sysexits.h conventions.
 */
public final class ExitCodes {

    private ExitCodes() {}

    /** Program completed successfully. */
    public static final int OK = 0;

    /** Runtime or parse error. */
    public static final int ERROR = 1;

    /** Suspended at checkpoint (temporary failure, state saved). */
    public static final int SUSPENDED = 75;
}
