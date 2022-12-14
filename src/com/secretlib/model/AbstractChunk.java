package com.secretlib.model;

import com.secretlib.exception.TruncatedBagException;
import com.secretlib.util.Log;

import java.util.Arrays;

/**
 * @author FFRADET
 */
public abstract class AbstractChunk {

    private static final Log LOG = new Log(AbstractChunk.class);
    private static int serialId = 0;

    protected EChunk type;
    private int id;

    // The raw data
    protected byte[] rawData = new byte[0];

    protected AbstractChunk() {
        id = serialId++;
    }

    public static AbstractChunk createFrom(byte[] all, int offset) {
        LOG.begin("createFrom");
        AbstractChunk item = null;
        try {
            EChunk type = EChunk.valueOf(all, offset);
            if (type != null) {
                item = type.getClassName().getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        LOG.end("createFrom");
        return item;
    }

    public EChunk getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public int getLength() {
        return (this.getRawData() != null) ? this.getRawData().length : 0;
    }

    public int getTotalLength() {
        return 1 + 3 + getLength(); // Type, length, data
    }

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public static int readInt8(byte[] all, int offset) throws TruncatedBagException {
        if (offset + 1 > all.length)
            throw new TruncatedBagException();

        int val = (int) (all[offset] & 0xff);
        return val;
    }

    public static int readInt24(byte[] all, int offset) throws TruncatedBagException {
        if (offset + 3 > all.length)
            throw new TruncatedBagException();

        int val = (int) (all[offset] & 0xff) + (((int) all[offset + 1] & 0xff) << 8) + (((int) all[offset + 2] & 0xff) << 16);
        return val;
    }

    public int parse(byte[] all, int offset) throws TruncatedBagException {
        int len = readInt24(all, offset);
        offset += 3;

        if (offset + len > all.length)
            throw new TruncatedBagException();

        rawData = Arrays.copyOfRange(all, offset, offset + len);
        offset += len;

        return offset;
    }

    public byte[] toByteArray() {
        int len = getLength();
        int l = getTotalLength();
        byte[] all = new byte[l];
        all[0] = type.getType();
        all[1] = (byte) ((int) len & 0xFF);
        all[2] = (byte) ((int) (len >> 8) & 0xFF);
        all[3] = (byte) ((int) (len >> 16) & 0xFF);

        if (len > 0) {
            System.arraycopy(this.getRawData(), 0, all, 4, len);
        }

        return all;
    }

    @Override
    public String toString() {
        return "AbstractChunk{" +
                "type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractChunk item = (AbstractChunk) o;

        return id == item.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
