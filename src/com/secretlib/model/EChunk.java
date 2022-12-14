package com.secretlib.model;

import com.secretlib.exception.TruncatedBagException;
import com.secretlib.util.Log;

import java.util.EnumSet;

/**
 * @author Florent FRADET
 */
public enum EChunk {

    STOP((byte) 0x00, ChunkStop.class),
    DATA((byte) 0x01, ChunkData.class);

    private static final Log LOG = new Log(EChunk.class);


    private byte type;
    private Class<? extends AbstractChunk> className;

    public static EnumSet<EChunk> VALUES = EnumSet.allOf(EChunk.class);

    private EChunk(byte type, Class<? extends AbstractChunk> className) {
        this.type = type;
        this.className = className;
    }

    public Class<? extends AbstractChunk> getClassName() {
        return className;
    }

    public byte getType() {
        return type;
    }

    public static EChunk valueOf(byte[] data, int offset) throws TruncatedBagException {
        if (offset >= data.length)
            throw new TruncatedBagException();

        byte type = data[offset];
        for (EChunk i : VALUES) {
            if (i.type == type) {
                LOG.debug("Chunk identified type : " + i.type);
                return i;
            }
        }
        LOG.warn("Chunk type unknown");
        return null;
    }
}
