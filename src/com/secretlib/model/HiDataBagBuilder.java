package com.secretlib.model;

import com.secretlib.exception.BagParseFinishException;
import com.secretlib.exception.NoBagException;
import com.secretlib.exception.TruncatedBagException;
import com.secretlib.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
public class HiDataBagBuilder extends OutputStream {
    private static final Log LOG = new Log(HiDataBagBuilder.class);

    private ByteArrayOutputStream buf;
    private HiDataBag bag;

    private enum EStatus {
        HEADER, CHUNK_TYPE, CHUNK_LENGTH, CHUNK_DATA
    }

    private EStatus state;
    private int idx;
    private int expected;
    private AbstractChunk curChunk;


    public HiDataBagBuilder(HiDataBag bag) {
        this.bag = bag;
        clear();
        state = EStatus.HEADER;
        expected = 0;
        curChunk = null;
    }

    private void clear() {
        buf = new ByteArrayOutputStream();
        idx = 0;
    }

    @Override
    public void write(int b) throws IOException {
        if (state == EStatus.HEADER) {
            if (b != HiDataBag.BAG_HEADER[idx]) {
                throw new NoBagException();
            }
            idx++;
            if (idx == HiDataBag.BAG_HEADER.length) {
                state = EStatus.CHUNK_TYPE;
                idx = 0;
            }
        } else if (state == EStatus.CHUNK_TYPE) {
            buf.write(b);
            curChunk = AbstractChunk.createFrom(buf.toByteArray(), 0);
            if (curChunk instanceof ChunkStop) {
                throw new BagParseFinishException();
            }
            if (curChunk == null)
                throw new NoBagException();
            bag.addItem(curChunk);
            clear();
            state = EStatus.CHUNK_LENGTH;
        } else if (state == EStatus.CHUNK_LENGTH) {
            buf.write(b);
            idx++;
            if (idx == 3) {
                expected = AbstractChunk.readInt24(buf.toByteArray(), 0);
                LOG.debug("Expected chunk size : " + expected);
                clear();
                state = EStatus.CHUNK_DATA;
            }
        } else if (state == EStatus.CHUNK_DATA) {
            buf.write(b);
            idx++;
            if (idx == expected) {
                expected = 0;
                curChunk.setRawData(buf.toByteArray());
                clear();
                state = EStatus.CHUNK_TYPE;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if ((expected > 0) && (idx < expected))
            throw new TruncatedBagException();

//        bag.parse(buf.toByteArray());
    }
}
