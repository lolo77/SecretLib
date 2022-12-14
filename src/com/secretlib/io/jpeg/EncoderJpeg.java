package com.secretlib.io.jpeg;

import com.secretlib.exception.HiDataEncoderException;
import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.BatchParameters;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;
import com.secretlib.util.HiUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class EncoderJpeg {
    private static final Log LOG = new Log(EncoderJpeg.class);


    public boolean encode(Frame imgFrame, byte[] data, Parameters params) throws HiDataEncoderException {
        LOG.begin("encode");

        boolean bInterrupted = false;

        if (data.length > 0) {
            int bit = 1;
            int idxData = 0;
            int bitMask = 1 << params.getBitStart();
            int bitStart = params.getBitStart();
            byte[] pass = params.getKm().getBytes(StandardCharsets.UTF_8);
            byte[] hash = HiUtils.genHash(pass, params.getHashAlgo());
            WalkerJpeg w = new WalkerJpeg(imgFrame, hash, bitStart);

            int idxHash = (pass.length > 0) ? (((int)pass[0]) & 0xff) : 0xff;
            idxHash %= hash.length;

            long timeStart = System.currentTimeMillis();
            ProgressMessage msg = new ProgressMessage(ProgressStepEnum.ENCODE, 0);
            msg.setNbBitsTotals(w.getDctUsableCounts());
            msg.setNbBitsCapacity(data.length * 8);

            byte curByte = (byte) (data[0] ^ hash[idxHash]);
            int nbBitsStorable = (params.isAutoExtendBit()) ? w.getDctUsableCountExtend() : w.getDctUsableCount();
            while (w.getIdxDataBit() < data.length * 8) {

                boolean flag = (curByte & bit) > 0;

                if (flag) {
                    w.setBitDct(bitMask);
                } else {
                    w.clearBitDct(bitMask);
                }

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

                //LOG.debug("write bit " + (flag ? "1" : "0") + " ; nbBitsChanged : " + w.getNbBitsChanged());

                if ((idxData & 0x0fff) == 0) {
                    long timeCur = System.currentTimeMillis();
                    if (timeCur - timeStart > 100) {
                        timeStart = timeCur;
                        msg.setProgress((double) w.getIdxDataBit() / (double) (data.length * 8));
                        msg.setNbBitsChanged(w.getNbBitsChanged());
                        msg.setNbBitsUsed(w.getIdxDataBit());
                        params.getProgressCallBack().update(msg);
                    }
                }

                if (!w.inc()) {
                    if (params.isAutoExtendBit()) {
                        bitStart++;
                        bitMask <<= 1;
                        w.setBitNum(bitStart); // Will output a warning if bitStart > 7
                        w.clearUsedIdx();
                        w.inc();
                        if (bitStart == 8) {
                            bInterrupted = true;
                            break;
                        }
                    } else {
                        bInterrupted = true;
                        break;
                    }
                }
            }

            int nbBitsStored = 0;
            int nbBitsChanged = 0;

            if (!bInterrupted) {
                nbBitsStored = w.getIdxDataBit();
                nbBitsChanged = w.getNbBitsChanged();

                LOG.debug("Nb bits stored  : " + nbBitsStored);
                LOG.debug("Nb bits changed : " + nbBitsChanged);
            }

            msg.setProgress((double) w.getIdxDataBit() / (double) (data.length * 8));
            msg.setNbBitsUsed(nbBitsStored);
            msg.setNbBitsChanged(nbBitsChanged);
            params.getProgressCallBack().update(msg);
        }
        LOG.end("encode returns " + (!bInterrupted));
        return !bInterrupted;
    }


    // Debug only
    public static void main(String[] args) {
        Log.setLevel(Log.TRACE);

        ImageIO.setUseCache(false);

        BatchParameters params = new BatchParameters(args);
        File f = new File(params.getFileNameImgCarrier());

        InputStream in = null;
        ReaderJpeg reader = null;
        try {
            FileInputStream fis = new FileInputStream(f);
            reader = new ReaderJpeg(HiUtils.readAllBytes(fis), params);
            reader.decode();
        } catch (Exception e) {
            try {
                LOG.warn("Carrier image is being rewrited.");
                // Read the original JPG
                BufferedImage img = ImageIO.read(f);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                // Write it as a clean image since JPG can hold multiple progressive scans
                // Progressive scans are not easy to deal with
                ImageIO.write(img, "jpg", bos);

                reader = new ReaderJpeg(bos.toByteArray(), params);
                reader.decode();
            }
            catch (Exception e2) {
                LOG.error("Unable to read img");
                return;
            }
        }
        try {
            Log.setLevel(Log.TRACE);
            byte[] data = new StringBuffer("azertyuiopqsdfghjklmw").toString().getBytes(StandardCharsets.UTF_8);
            EncoderJpeg encoder = new EncoderJpeg();
            boolean b = encoder.encode(reader.getFrame(), data, params);
            if (!b) {
                LOG.error("Encoding interrupted");
            }

            Log.setLevel(Log.CRITICAL);
            FileOutputStream fos = new FileOutputStream(new File("res/output.jpg"));
            WriterJpeg writer = new WriterJpeg(reader.getFrame(), fos, params);
            writer.write();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.crit(e.getMessage());
        }

    }
}
