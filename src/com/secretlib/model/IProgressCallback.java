package com.secretlib.model;

/**
 * @author Florent FRADET
 *
 * Callback invoked from inside encode/decode jobs process
 */
public interface IProgressCallback {
    public void update(ProgressMessage msg);
}
