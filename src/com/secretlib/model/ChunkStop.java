package com.secretlib.model;

/**
 * @author FFRADET
 */
public class ChunkStop extends AbstractChunk {

    public ChunkStop() {
        super();
        type = EChunk.STOP;
    }

    @Override
    public String toString() {
        return "ChunkStop";
    }
}
