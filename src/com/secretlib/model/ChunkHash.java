package com.secretlib.model;

import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Florent FRADET
 * <p>
 * Hash struture :
 * <p>
 * Chunk type : 1 byte
 * Chunk length : 3 bytes
 * Hash algo implementation name : 1 byte {impNameLen}
 * Hash name (UTF-8) : {impNameLen} bytes
 * Hash data (binary) : Chunk Length - 4 - {impNameLen}
 * <p>
 * If HEADER is clearly readable : data is not encrypted
 * If not, try to uncipher the data. If HEADER is still not readable : data remains encrypted
 */
public class ChunkHash extends AbstractChunk {

    private static final Log LOG = new Log(ChunkHash.class);

    private static final String DEFAULT_HASH_IMPLEMENTATION_NAME = "SHA-256";

    // Hash implementation name. Ex: "SHA-256"
    private String hashName = DEFAULT_HASH_IMPLEMENTATION_NAME;

    // The binary hash data

    public ChunkHash() {
        super();
        type = EChunk.HASH;
    }

    protected byte[] concatRawData(HiDataBag bag) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        for (AbstractChunk c : bag.getItems()) {
            if (c instanceof ChunkData) {
                try {
                    buf.write(c.getRawData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return buf.toByteArray();
    }

    public void computeHash(HiDataBag parent) throws IllegalArgumentException {

        byte[] theBag = concatRawData(parent);
        byte[] hashData = HiUtils.digest(theBag, hashName);
        byte[] bufName = hashName.getBytes(StandardCharsets.UTF_8);
        int len = 1 + bufName.length;

        byte[] all = new byte[len + hashData.length];
        int ofs = 0;
        all[ofs++] = (byte) bufName.length;
        System.arraycopy(bufName, 0, all, ofs, bufName.length);
        ofs += bufName.length;
        System.arraycopy(hashData, 0, all, ofs, hashData.length);

        rawData = all;
    }


    public boolean verifyHash(HiDataBag parent) throws IllegalArgumentException {
        if (rawData.length == 0) {
            throw new IllegalArgumentException("Can not verify hash : ChunkHash item not loaded");
        }
        byte[] bufHashName = new byte[rawData[0]];
        byte[] bufHashData = new byte[rawData.length - rawData[0] - 1];
        byte[] theBag = concatRawData(parent);

        System.arraycopy(rawData, 1, bufHashName, 0, bufHashName.length);
        System.arraycopy(rawData, 1 + bufHashName.length, bufHashData, 0, bufHashData.length);
        hashName = new String(bufHashName, StandardCharsets.UTF_8);

        LOG.debug("HashName : " + hashName);
        LOG.debug("Loaded   HashData : " + HiUtils.toStringHex(bufHashData, 16));

        byte[] bufHash = HiUtils.digest(theBag, hashName);
        LOG.debug("Computed HashData : " + HiUtils.toStringHex(bufHash, 16));

        return HiUtils.arraysEquals(bufHashData, 0, bufHash, 0, bufHashData.length);
    }

    public String getHashName() {
        return hashName;
    }

    public void setHashName(String hashName) {
        if (hashName != null) {
            this.hashName = hashName;
        }
    }


    @Override
    public String toString() {
        return "ChunkHash{" +
                "hashName=" + hashName +
                "}";
    }
}
