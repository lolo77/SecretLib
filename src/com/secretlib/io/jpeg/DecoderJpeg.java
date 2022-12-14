package com.secretlib.io.jpeg;

import com.secretlib.exception.BagParseFinishException;
import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DecoderJpeg {

    private static final Log LOG = new Log(DecoderJpeg.class);

    public static void decode(Frame imgFrame, Parameters params, OutputStream out) throws IOException {
        LOG.begin("decode");

        int bitStart = params.getBitStart();
        int bitMask = 1 << params.getBitStart();
        byte[] pass = params.getKm().getBytes(StandardCharsets.UTF_8);
        byte[] hash = HiUtils.genHash(pass, params.getHashAlgo());
        WalkerJpeg w = new WalkerJpeg(imgFrame, hash, bitStart);
        byte b = 0;
        int idxByte = 0;
        int idxHash = (pass.length > 0) ? (((int) pass[0]) & 0xff) : 0xff;
        idxHash %= hash.length;

        int iDctUsableCountExt = (params.isAutoExtendBit()) ? w.getDctUsableCountExtend() : w.getDctUsableCount();

        long timeStart = System.currentTimeMillis();
        ProgressMessage msg = new ProgressMessage(ProgressStepEnum.DECODE, 0);
        msg.setNbBitsTotals(w.getDctUsableCounts());
        msg.setNbBitsCapacity(iDctUsableCountExt);
        params.getProgressCallBack().update(msg);

        try {
            int bit = 1;
            while (true) {

                if (w.isBitDct(bitMask)) {
                    b |= bit;
                }

                if (bit == 0x80) {
                    b ^= hash[idxHash];
                    idxHash++;
                    idxHash %= hash.length;

                    out.write(b);

                    bit = 1;
                    b = 0;
                    idxByte++;

                    if ((idxByte & 0x0fff) == 0) {
                        long timeCur = System.currentTimeMillis();
                        if (timeCur - timeStart > 100) {
                            timeStart = timeCur;
                            msg.setProgress((double) w.getIdxDataBit() / (double) (iDctUsableCountExt));
                            msg.setNbBitsUsed(w.getIdxDataBit());
                            params.getProgressCallBack().update(msg);
                        }
                    }
                } else {
                    bit <<= 1;
                }
                //LOG.debug("write bit " + (flag ? "1" : "0") + " ; nbBitsChanged : " + w.getNbBitsChanged());

                if (!w.inc()) {
                    if (params.isAutoExtendBit()) {
                        bitStart++;
                        bitMask <<= 1;
                        w.setBitNum(bitStart);
                        w.clearUsedIdx();
                        w.inc();
                        if (bitStart > 7) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            out.write(b);
        } catch (IOException e) {
            if (!(e instanceof BagParseFinishException)) {
                msg.setNbBitsUsed(0);
                params.getProgressCallBack().update(msg);
                LOG.end("decode - Exception to be thrown : " + e.getMessage());
                throw e;
            }
        }
        msg.setProgress((double) w.getIdxDataBit() / (double) (iDctUsableCountExt));
        msg.setNbBitsUsed(w.getIdxDataBit());
        params.getProgressCallBack().update(msg);

        LOG.end("decode");
    }
}