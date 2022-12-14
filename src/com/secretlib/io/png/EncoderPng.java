package com.secretlib.io.png;

import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;
import com.secretlib.util.HiUtils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.charset.StandardCharsets;

/**
 * @author FFRADET
 */
public class EncoderPng {

    private static final Log LOG = new Log(EncoderPng.class);


    public boolean encode(BufferedImage img, byte[] data, Parameters params) {
        LOG.begin("encode");
        boolean bInterrupted = false;

        if (data.length > 0) {
            int nbComp = img.getColorModel().getNumComponents();
            byte[] pass = params.getKm().getBytes(StandardCharsets.UTF_8);
            byte[] hash = HiUtils.genHash(pass, params.getHashAlgo());
            WalkerPng w = new WalkerPng(hash, img.getWidth(), img.getHeight(), nbComp);

            int idxHash = (pass.length > 0) ? (((int) pass[0]) & 0xff) : 0xff;
            idxHash %= hash.length;

            int octMax = w.getBitMax() / 8;
            int nbChanged = 0;
            LOG.debug("Total space per pixel's component bit (bytes) = " + octMax);
            LOG.debug("Space required (bytes) = " + data.length);

            LOG.debug("Writing data : " + HiUtils.toStringHex(data, 16));

            WritableRaster r = img.getRaster();

            int[] pix = new int[nbComp];

            long timeStart = System.currentTimeMillis();
            ProgressMessage msg = new ProgressMessage(ProgressStepEnum.ENCODE, 0);
            for (int i = 0; i < 8; i++) {
                msg.setNbBitsTotal(w.getBitMax(), i);
            }

            msg.setNbBitsCapacity(data.length * 8);

            int bit = 1;
            int idxData = 0;
            int bitMask = 1 << params.getBitStart();
            byte curByte = (byte) (data[0] ^ hash[idxHash]);
            while (w.getIdxDataBit() < data.length * 8) {
                int x = w.getTmpPixX();
                int y = w.getTmpPixY();
                r.getPixel(x, y, pix);

                boolean flag = (curByte & bit) > 0;

                int oldVal = pix[w.getTmpPixComp()];
                if (flag) {
                    if ((oldVal & bitMask) == 0) {
                        pix[w.getTmpPixComp()] |= bitMask;
                        nbChanged++;
                    }
                } else {
                    if ((oldVal & bitMask) != 0) {
                        pix[w.getTmpPixComp()] &= ~bitMask;
                        nbChanged++;
                    }
                }

                r.setPixel(x, y, pix);

                if (bit == 0x80) {
                    idxData++;
                    if (idxData < data.length) {
                        idxHash++;
                        idxHash %= hash.length;
                        curByte = (byte) (data[idxData] ^ hash[idxHash]);
                    }
                    bit = 1;
                } else {
                    bit <<= 1;
                }

                if ((idxData & 0x01ff) == 0) {
                    long timeCur = System.currentTimeMillis();
                    if (timeCur - timeStart > 100) {
                        timeStart = timeCur;
                        msg.setProgress((double) idxData / (double) (data.length));
                        msg.setNbBitsChanged(nbChanged);
                        msg.setNbBitsUsed(w.getIdxDataBit());
                        params.getProgressCallBack().update(msg);
                    }
                }

                if (!w.inc()) {
                    if (params.isAutoExtendBit()) {
                        bitMask <<= 1;
                        w.clearUsedIdx();
                        w.inc();
                        if (bitMask > 0x80) {
                            // No more space
                            bInterrupted = true;
                            break;
                        }
                    } else {
                        // No more space
                        bInterrupted = true;
                        break;
                    }
                }
            }

            int nbBitsStored = 0;
            int nbBitsChanged = 0;

            if (!bInterrupted) {
                nbBitsStored = w.getIdxDataBit();
                nbBitsChanged = nbChanged;
            }

            msg.setProgress((double) idxData / (double) (data.length));
            msg.setNbBitsUsed(nbBitsStored);
            msg.setNbBitsChanged(nbBitsChanged);
            params.getProgressCallBack().update(msg);
        }
        LOG.end("encode return " + (!bInterrupted));
        return !bInterrupted;
    }

}
