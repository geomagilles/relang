package com.relang.nodes;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Table tracking all awaitables in an execution.
 * Serializable for inclusion in snapshots.
 */
public class AwaitableTable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, AwaitableHandle> handles = new HashMap<>();

    public void register(AwaitableHandle handle) {
        handles.put(handle.getId(), handle);
    }

    public AwaitableHandle get(String id) {
        return handles.get(id);
    }

    public Map<String, AwaitableHandle> getAll() {
        return handles;
    }

    public boolean isEmpty() {
        return handles.isEmpty();
    }
}
