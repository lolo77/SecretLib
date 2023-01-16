package com.secretlib.io.pdf;

import java.util.Arrays;

/**
 *
 */
public class WalkerPdf {

    private int idxHash;
    private byte[] hash;
    private byte[] usedIdx = null;
    private long nbUsed = 0;
    private int nbParts;
    private int idxCur = 0;

    /**
     *
     * @param hash
     * @param nbParts
     */
    public WalkerPdf(byte[] hash, int nbParts) {
        this.hash = hash;
        this.nbParts = nbParts;

        usedIdx = new byte[(nbParts)/8+1];
        clearUsedIdx();

        idxHash = Math.abs((int)hash[0]);
        idxCur = idxHash;
        adjust();
    }

    public void clearUsedIdx() {
        Arrays.fill(usedIdx, (byte)0);
        nbUsed = 0;
    }

    public boolean inc() {
        if (nbUsed >= nbParts) {
            return false;
        }
        idxHash %= hash.length;
        idxCur += hash[idxHash++] & 0xFF;
        adjust();

        int iUsedIdxByte = idxCur >> 3;
        int iUsedIdxBit = 1 << (idxCur & 7);
        usedIdx[iUsedIdxByte] |= iUsedIdxBit;
        nbUsed++;

        return true;
    }

    private void adjust() {
        idxCur %= nbParts;
        int iUsedIdxByte = idxCur >> 3;
        int iUsedIdxBit = 1 << (idxCur & 7);

        while ((usedIdx[iUsedIdxByte] & iUsedIdxBit) > 0) {
            idxCur++;
            while (idxCur >= nbParts) {
                idxCur -= nbParts;
            }
            iUsedIdxByte = idxCur >> 3;
            iUsedIdxBit = 1 << (idxCur & 7);
        }
    }

    public int getIdxCur() {
        return idxCur;
    }
}
