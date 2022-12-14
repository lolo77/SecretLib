package com.secretlib.model;

import com.secretlib.exception.NoBagException;
import com.secretlib.exception.TruncatedBagException;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Florent FRADET
 * <p>
 * The data holding structure
 */
public class HiDataBag {

    private static final Log LOG = new Log(HiDataBag.class);

    public static final byte[] BAG_HEADER = {(byte) 0xEC, (byte) 0x95, (byte) 0x88, (byte) 0xEB, (byte) 0x85, (byte) 0x95, (byte) 0x46, (byte) 0x46};

    private List<AbstractChunk> items = new ArrayList<>();

    public HiDataBag() {
        clear();
    }


    public void clear() {
        items.clear();
        items.add(new ChunkStop());
    }


    /**
     * @return true if there is no data
     */
    public boolean isEmpty() {
        return items.isEmpty() || ((items.size() == 1) && (items.get(0).getType().equals(EChunk.STOP)));
    }

    /**
     * @param id the DataFrag id
     * @return the DataFrag instance. Returns null if not found
     */
    public AbstractChunk findById(int id) {
        for (AbstractChunk d : items) {
            if (d.getId() == id) {
                return d;
            }
        }
        return null;
    }

    /**
     * @param id the DataFrag id to remove
     * @return true if item removed
     */
    public boolean removeById(int id) {
        boolean b = false;
        Iterator<AbstractChunk> iter = items.iterator();
        while (iter.hasNext()) {
            AbstractChunk d = iter.next();
            if (d.getId() == id) {
                iter.remove();
                b = true;
            }
        }
        return b;
    }

    /**
     * @param ids the list of DataFrag id to remove
     * @return the number of removed items
     */
    public int removeAllById(List<Integer> ids) {
        int nb = 0;
        Iterator<AbstractChunk> iter = items.iterator();
        while (iter.hasNext()) {
            AbstractChunk d = iter.next();
            if (ids.contains(d.getId())) {
                iter.remove();
                nb++;
            }
        }
        return nb;
    }

    /**
     * @param data the raw bytes to parse
     */
    public void parse(byte[] data) throws TruncatedBagException, NoBagException {
        LOG.begin("parse");

        if (data.length <= BAG_HEADER.length + 1) {
            LOG.warn("No header");
            throw new NoBagException();
        }

        byte[] header = Arrays.copyOfRange(data, 0, BAG_HEADER.length);
        if (!Arrays.equals(header, BAG_HEADER)) {
            LOG.warn("Header mismatch");
            throw new NoBagException();
        }

        items = new ArrayList<>();
        int idx = BAG_HEADER.length;
        while (idx < data.length) {
            AbstractChunk item = AbstractChunk.createFrom(data, idx);
            if (item == null) {
                LOG.warn("DataFrag unknown");
                throw new NoBagException();
            }
            items.add(item);
            idx = item.parse(data, idx + 1);
            LOG.debug(item.toString());
            if (EChunk.STOP.equals(item.getType())) {
                break;
            }
        }
        LOG.end("parse");
}


    /**
     * Add a data fragment to the bag
     *
     * @param item the data fragment
     * @return the fragment index
     */
    public int addItem(AbstractChunk item) {
        LOG.begin("addItem");
        int idx = 0;
        if (items.size() > 0) {
            AbstractChunk last = items.get(items.size() - 1);
            if (last instanceof ChunkStop) {
                idx = items.size() - 1;
            }
        }
        items.add(idx, item);
        LOG.debug("Added chunk " + item.toString() + " at index " + idx);
        LOG.end("addItem returns " + idx);
        return idx;
    }

    /**
     * Get the length of the entire bag
     *
     * @return the length in bytes
     */
    public int getLength() {
        LOG.begin("getLength");
        int len = BAG_HEADER.length;
        for (AbstractChunk i : items) {
            len += i.getTotalLength();
        }
        LOG.end("getLength returns " + len);
        return len;
    }

    public void encryptAll(Parameters p) throws GeneralSecurityException {
        for (AbstractChunk item : items) {
            if (item instanceof ChunkData) {
                ChunkData itemData = (ChunkData) item;
                itemData.encryptData(p);
            }
        }
    }

    public void decryptAll(Parameters p) throws GeneralSecurityException, TruncatedBagException {
        for (AbstractChunk item : items) {
            if (item instanceof ChunkData) {
                ChunkData itemData = (ChunkData) item;
                itemData.decryptData(p);
            }
        }
    }

    /**
     * Converts the bag to a raw byte array
     * /!\ Call encryptAll() before toByteArray() to ensure rawData is up-to-date
     *
     * @return the serialized bag
     */
    public byte[] toByteArray() {
        LOG.begin("toByteArray");

        int len = getLength();

        byte[] all = new byte[len];
        System.arraycopy(BAG_HEADER, 0, all, 0, BAG_HEADER.length);
        int ofs = BAG_HEADER.length;

        for (AbstractChunk i : items) {
            byte[] itemBytes = i.toByteArray();
            System.arraycopy(itemBytes, 0, all, ofs, i.getTotalLength());
            ofs += i.getTotalLength();
        }

        LOG.debug("");

        LOG.end("toByteArray");

        return all;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("HiDataBag{");
        boolean bFirst = true;
        for (AbstractChunk i : items) {
            if (!bFirst) {
                s.append(",\n");
            }
            s.append(i.toString());
            bFirst = false;
        }
        s.append("}");
        return s.toString();
    }

    /**
     * Get a list of the data fragments
     *
     * @return the list
     */
    public List<AbstractChunk> getItems() {
        return items;
    }
}
