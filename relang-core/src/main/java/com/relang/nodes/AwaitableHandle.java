package com.relang.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Runtime representation of an awaitable (*T).
 * Has a unique id, status, and optional result.
 * Registered in the AwaitableTable for tracking and persistence.
 */
@ExportLibrary(InteropLibrary.class)
public class AwaitableHandle implements TruffleObject, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Status { PENDING, RESOLVED, FAILED }

    private final String id;
    private final long createdAt;
    private Status status;
    private Object result;

    public AwaitableHandle() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.status = Status.PENDING;
    }

    public AwaitableHandle(String id, long createdAt, Status status, Object result) {
        this.id = id;
        this.createdAt = createdAt;
        this.status = status;
        this.result = result;
    }

    public String getId() { return id; }
    public long getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public Object getResult() { return result; }

    public boolean isResolved() { return status == Status.RESOLVED; }
    public boolean isPending() { return status == Status.PENDING; }
    public boolean isFailed() { return status == Status.FAILED; }

    public void resolve(Object value) {
        this.status = Status.RESOLVED;
        this.result = value;
    }

    public void fail(Object failure) {
        this.status = Status.FAILED;
        this.result = failure;
    }

    @ExportMessage
    boolean hasMembers() { return true; }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return new String[]{"id", "createdAt", "status"};
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return "id".equals(member) || "createdAt".equals(member) || "status".equals(member);
    }

    @ExportMessage
    Object readMember(String member) {
        return switch (member) {
            case "id" -> id;
            case "createdAt" -> createdAt;
            case "status" -> status.name();
            default -> throw new UnsupportedOperationException("Unknown member: " + member);
        };
    }

    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaObject() {
        return new ReLangMetaType("Awaitable");
    }

    @ExportMessage
    @SuppressWarnings("unused")
    String toDisplayString(boolean allowSideEffects) {
        return "*Awaitable(" + id.substring(0, 8) + "... " + status + ")";
    }
}
