package com.secretlib.io.png;

import java.util.Arrays;

/**
 * @author FFRADET 
 */
public class WalkerPng {
    // Current values to compute coordinates
    private int idxDataBit;
    private int idxHash;
    private int idxPixelComp;
    private byte[] hash;
    private byte[] usedIdx = null;
    private long nbUsed = 0;

    // Input fixed values
    private int imgWidth;
    private int imgHeight;
    private int imgNbComp;
    private int bitMax;

    // Coordinates
    private int tmpPixX = 0;
    private int tmpPixY = 0;
    private int tmpPixComp = 0;


    public WalkerPng(byte[] hash, int w, int h, int nbComp) {
        this.hash = hash;

        idxHash = Math.abs((int)hash[0]);
        idxHash %= hash.length;

        idxPixelComp = Math.abs(hash[idxHash]);
        idxDataBit = 0;
        usedIdx = new byte[(w*h*nbComp)/8+1];
        updateInitParams(w, h, nbComp);
    }

    public void clearUsedIdx() {
        Arrays.fill(usedIdx, (byte)0);
        nbUsed = 0;
    }

    public boolean inc() {
        if (nbUsed >= bitMax) {
            return false;
        }
        idxHash %= hash.length;
        idxPixelComp += hash[idxHash++] & 0xFF;
        adjust();

        int iUsedIdxByte = idxPixelComp >> 3;
        int iUsedIdxBit = 1 << (idxPixelComp & 7);
        usedIdx[iUsedIdxByte] |= iUsedIdxBit;
        nbUsed++;

        idxDataBit++;

        updateTmp();
        return true;
    }


    private void adjust() {
        idxPixelComp %= bitMax;
        int iUsedIdxByte = idxPixelComp >> 3;
        int iUsedIdxBit = 1 << (idxPixelComp & 7);

        while ((usedIdx[iUsedIdxByte] & iUsedIdxBit) > 0) {
            idxPixelComp++;
            while (idxPixelComp >= bitMax) {
                idxPixelComp -= bitMax;
            }
            iUsedIdxByte = idxPixelComp >> 3;
            iUsedIdxBit = 1 << (idxPixelComp & 7);
        }
    }


    private void updateTmp() {
        int linearPix = idxPixelComp / imgNbComp;
        tmpPixComp = (idxPixelComp % imgNbComp);
        tmpPixX = linearPix % imgWidth;
        tmpPixY = linearPix / imgWidth;
    }


    public void updateInitParams(int w, int h, int nbComp) {
        imgWidth = w;
        imgHeight = h;
        imgNbComp = nbComp;
        bitMax = w * h * nbComp;

        clearUsedIdx();
        adjust();

        int iUsedIdxByte = idxPixelComp >> 3;
        int iUsedIdxBit = 1 << (idxPixelComp & 7);
        usedIdx[iUsedIdxByte] |= iUsedIdxBit;
        nbUsed++;

        updateTmp();
    }

    public int getBitMax() {
        return bitMax;
    }

    public int getTmpPixX() {
        return tmpPixX;
    }

    public int getTmpPixY() {
        return tmpPixY;
    }

    public int getTmpPixComp() {
        return tmpPixComp;
    }

    public int getIdxDataBit() {
        return idxDataBit;
    }
}
