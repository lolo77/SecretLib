package com.secretlib.io.jpeg;

import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;

import java.util.Arrays;
import java.util.HashSet;

public class WalkerJpeg {
    private static final Log LOG = new Log(WalkerJpeg.class);

    // Current values to compute coordinates
    private int idxDataBit;
    private int idxHash;
    private int idxDct;
    private byte[] hash;
    private HashSet<Integer> usedIdx = new HashSet<>();
    private BlockDct[] allBlocks = null;
    private Frame imgFrame;
    private int nbBitsChanged = 0;

    // Input fixed values
    private int dctTotalCount;
    private int[] dctUsableCounts;
    private int dctSize = 64;
    private int dctThreshold;
    private int bitNum;

    // Coordinates
    private int tmpBlk = 0;
    private int tmpDct = 0;


    public WalkerJpeg(Frame imgFrame, byte[] hash, int bitNum) {
        this.hash = hash;
        this.imgFrame = imgFrame;

        idxHash = Math.abs((int) hash[0]);
        idxHash %= hash.length;

        idxDct = Math.abs(hash[idxHash]);
        idxDataBit = 0;
        setBitNum(bitNum);
        updateInitParams();

        usedIdx = new HashSet<>();
        adjust();
        usedIdx.add(idxDct);
    }


    public void clearUsedIdx() {
        usedIdx.clear();
    }

    public boolean inc() {
        if (usedIdx.size() >= dctUsableCounts[bitNum]) {
            return false;
        }
        idxHash %= hash.length;
        idxDct += (int) hash[idxHash++] & 0xFF;
        adjust();
        usedIdx.add(idxDct);
        idxDataBit++;

        updateTmp();
        return true;
    }


    private void adjust() {
        while (true) {
            idxDct %= dctTotalCount;
            updateTmp();
            while ((allBlocks[tmpBlk].dct[tmpDct] & dctThreshold) == 0) {
                idxDct++;
                idxDct %= dctTotalCount;
                updateTmp();
            }

            if (usedIdx.contains(idxDct)) {
                idxDct++;
            } else {
                break;
            }
        }
//        LOG.debug("new idxDct = " + idxDct);
//        LOG.debug("   Blk/Dct = " + tmpBlk + " / " + tmpDct);
    }


    private void updateTmp() {
        tmpBlk = idxDct / dctSize;
        tmpDct = idxDct % dctSize;
    }

    public void setBitNum(int bitNum) {

        if (bitNum < 0) {
            bitNum = 0;
            LOG.warn("bitNum set to 0");
        }
        if (bitNum > 7) {
            bitNum = 7;
            LOG.warn("bitNum set to 7");
        }

        this.bitNum = bitNum;

        int usableBits = bitNum + 1;
        dctThreshold = (-1) << usableBits;

        LOG.debug("dctThreshold = " + HiUtils.intToBin(dctThreshold));
    }

    public int getDctUsableCount() {
        return dctUsableCounts[bitNum];
    }

    public int getDctUsableCountExtend() {
        int nb = 0;
        for (int i = bitNum; i < 8; i++) {
            nb += dctUsableCounts[i];
        }
        return nb;
    }

    public int[] getDctUsableCounts() {
        return dctUsableCounts;
    }

    public int getIdxDataBit() {
        return idxDataBit;
    }

    public int getNbBitsChanged() {
        return nbBitsChanged;
    }

    public void setBitDct(int bitMask) {
//        LOG.debug("bit SET " + Utils.intToBin(bitMask) + " at block " + tmpBlk + " / coef " + tmpDct + " : prev value = " + Utils.intToBin(allBlocks[tmpBlk].dct[tmpDct]));

        if ((allBlocks[tmpBlk].dct[tmpDct] & bitMask) == 0) {
            allBlocks[tmpBlk].dct[tmpDct] |= bitMask;
//            LOG.debug("bit SET " + Utils.intToBin(bitMask) + " at block " + tmpBlk + " / coef " + tmpDct + " : new value = " + Utils.intToBin(allBlocks[tmpBlk].dct[tmpDct]));
            nbBitsChanged++;
        }
    }

    public void clearBitDct(int bitMask) {
//        LOG.debug("bit CLR " + Utils.intToBin(bitMask) + " at block " + tmpBlk + " / coef " + tmpDct + " : prev value = " + Utils.intToBin(allBlocks[tmpBlk].dct[tmpDct]));

        if ((allBlocks[tmpBlk].dct[tmpDct] & bitMask) != 0) {
            allBlocks[tmpBlk].dct[tmpDct] &= ~bitMask;
//            LOG.debug("bit CLR " + Utils.intToBin(bitMask) + " at block " + tmpBlk + " / coef " + tmpDct + " : new value = " + Utils.intToBin(allBlocks[tmpBlk].dct[tmpDct]));
            nbBitsChanged++;
        }
    }

    public boolean isBitDct(int bitMask) {
        return ((allBlocks[tmpBlk].dct[tmpDct] & bitMask) > 0);
    }

    private void updateInitParams() {

        int nbBlocks = 0;
        for (Component c : imgFrame.comps.values()) {
            nbBlocks += (c.blocks.length * c.blocks[0].length);
        }

        allBlocks = new BlockDct[nbBlocks];
        dctUsableCounts = new int[8];
        Arrays.fill(dctUsableCounts, 0);

        LOG.debug("NbBlocks : " + nbBlocks);
        int iBlk = 0;
        for (Component c : imgFrame.comps.values()) {
            for (int y = 0; y < c.blocks.length; y++) {
                for (int x = 0; x < c.blocks[y].length; x++) {
                    BlockDct blk = new BlockDct();
                    blk.x = x;
                    blk.y = y;
                    blk.dct = c.blocks[y][x];
                    allBlocks[iBlk++] = blk;

                    for (int i = 0; i < blk.dct.length; i++) {
                        int dctCoef = blk.dct[i];
                        for (int b = 0; b < 8; b++) {
                            int threshold = (-1) << b;
                            if ((dctCoef & threshold) > 0) {
                                dctUsableCounts[b]++;
                            }
                        }
                    }
                }
            }
        }

        dctSize = allBlocks[0].dct.length;
        dctTotalCount = nbBlocks * dctSize;

        int sumStorage = 0;
        for (int i = 0; i < 8; i++) {
            sumStorage += dctUsableCounts[i];
            LOG.debug("Max storable bits (" + i + ") : " + dctUsableCounts[i] + " (" + (dctUsableCounts[i] / 8) + " bytes)");
        }
        LOG.debug("Max storable bits (sum) : " + sumStorage + " (" + (sumStorage / 8) + " bytes)");
        LOG.debug("Nb total dct coefs : " + dctTotalCount + " (" + (dctTotalCount / 8) + " bytes)");
    }

}
