package com.secretlib.io.png;

import com.secretlib.exception.BagParseFinishException;
import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author FFRADET
 */
public class DecoderPng {

    private static final Log LOG = new Log(DecoderPng.class);

    public static void decode(BufferedImage img, Parameters params, OutputStream out) throws IOException {
        LOG.begin("decode");

        int nbComp = img.getColorModel().getNumComponents();
        byte[] pass = params.getKm().getBytes(StandardCharsets.UTF_8);
        byte[] hash = HiUtils.genHash(pass, params.getHashAlgo());
        WalkerPng w = new WalkerPng(hash, img.getWidth(), img.getHeight(), nbComp);
        int idxByte = 0;
        int idxHash = (pass.length > 0) ? (((int) pass[0]) & 0xff) : 0xff;
        idxHash %= hash.length;

        WritableRaster r = img.getRaster();
        int[] pix = new int[nbComp];

        int bitMask = 1 << params.getBitStart();
        byte oct = 0;
        int bit = 1;

        int nbBitsTotal = w.getBitMax();
        if (params.isAutoExtendBit()) {
            nbBitsTotal *= 8 - params.getBitStart();
        }

        long timeStart = System.currentTimeMillis();
        ProgressMessage msg = new ProgressMessage(ProgressStepEnum.DECODE, 0);
        for (int i = 0; i < 8; i++) {
            msg.setNbBitsTotal(w.getBitMax(), i);
        }
        msg.setNbBitsCapacity(nbBitsTotal);
        params.getProgressCallBack().update(msg);
        try {
            boolean bContinue = true;
            while (bContinue) {
                int x = w.getTmpPixX();
                int y = w.getTmpPixY();
                r.getPixel(x, y, pix);

                if ((pix[w.getTmpPixComp()] & bitMask) > 0) {
                    oct |= bit;
                }

                if (bit == 0x80) {
                    oct ^= hash[idxHash];
                    idxHash++;
                    idxHash %= hash.length;

                    out.write(oct);

                    oct = 0;
                    bit = 1;
                    idxByte++;

                    if ((idxByte & 0xfff) == 0) {
                        long timeCur = System.currentTimeMillis();
                        if (timeCur - timeStart > 100) {
                            timeStart = timeCur;
                            msg.setProgress((double) w.getIdxDataBit() / (double) (nbBitsTotal));
                            msg.setNbBitsUsed(w.getIdxDataBit());
                            params.getProgressCallBack().update(msg);
                        }
                    }
                } else {
                    bit <<= 1;
                }

                bContinue = w.inc();
                if ((!bContinue) && (params.isAutoExtendBit())) {
                    w.clearUsedIdx();
                    w.inc();
                    bitMask <<= 1;
                    if (bitMask <= 0x80) {
                        bContinue = true;
                    }
                }
            }
            out.write(oct);
        } catch (IOException e) {
            if (!(e instanceof BagParseFinishException)) {
                msg.setNbBitsUsed(0);
                params.getProgressCallBack().update(msg);
                LOG.end("decode - Exception to be thrown : " + e.getMessage());
                throw e;
            }
        }

        msg.setProgress((double) w.getIdxDataBit() / (double) (nbBitsTotal));
        msg.setNbBitsUsed(w.getIdxDataBit());
        params.getProgressCallBack().update(msg);

        LOG.end("decode");
    }
}
