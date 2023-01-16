package com.secretlib.io.stream;

import com.secretlib.exception.HiDataDecoderException;
import com.secretlib.model.HiDataBag;
import com.secretlib.model.AbstractChunk;
import com.secretlib.exception.TruncatedBagException;
import com.secretlib.util.Parameters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @Author : Florent FRADET
 *
 *  Read a steganographic image and try to extract the secret data from it
 */
public abstract class HiDataAbstractInputStream extends InputStream {
    private ByteArrayInputStream in = null;
    private HiDataBag bag = new HiDataBag();

    protected HiDataAbstractInputStream() {

    }

    public abstract List<String> getExtensions();
    public abstract boolean matches(byte[] in);
    public abstract HiDataAbstractInputStream create(InputStream in, Parameters p) throws IOException;

    /**
     *
     * @return the secret data bag
     */
    public HiDataBag getBag() { return bag; }

    /**
     *
     * @return the number of frags
     */
    public int getChunkCount() {
        return (bag != null) ? bag.getItems().size() : 0;
    }


    /**
     *
     * @param chunkNum the fragment index [0..getFragCount()-1]
     * @return the data fragment
     */
    public AbstractChunk getChunk(int chunkNum) {
        if (bag == null) {
            return null;
        }
        if ((chunkNum < 0) || (chunkNum >= getChunkCount())) {
            return null;
        }
        return bag.getItems().get(chunkNum);
    }

    /**
     *
     * @param chunkNum the fragment index [0..getFragCount()-1]
     * @return true if the stream is ready to be read
     */
    public boolean seek(int chunkNum) {
        if (bag == null) {
            return false;
        }
        if ((chunkNum < 0) || (chunkNum >= getChunkCount())) {
            return false;
        }
        in = new ByteArrayInputStream(bag.getItems().get(chunkNum).toByteArray());
        return true;
    }

    @Override
    public int read() throws IOException {
        if (in == null) {
            throw new HiDataDecoderException("DataFrag internal stream not open : call seek(n) before reading");
        }
        return in.read();
    }


    @Override
    public int available() throws IOException {
        if (in == null) {
            return 0;
        }
        return in.available();
    }
}
