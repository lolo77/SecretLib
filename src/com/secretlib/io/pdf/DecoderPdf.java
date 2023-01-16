package com.secretlib.io.pdf;

import com.itextpdf.kernel.pdf.*;
import com.secretlib.exception.BagParseFinishException;
import com.secretlib.exception.HiDataDecoderException;
import com.secretlib.exception.NoBagException;
import com.secretlib.exception.TruncatedBagException;
import com.secretlib.io.png.DecoderPng;
import com.secretlib.model.HiDataBag;
import com.secretlib.model.HiDataBagBuilder;
import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Florent FRADET
 */
public class DecoderPdf {

    private static final Log LOG = new Log(DecoderPdf.class);

    public static void decode(InputStream in, Parameters params, OutputStream out) throws IOException {
        byte[] pass = params.getKm().getBytes(StandardCharsets.UTF_8);
        byte[] hash = HiUtils.genHash(pass, params.getHashAlgo());

        byte[] hash2 = HiUtils.genHash(hash, params.getHashAlgo());
        String keyMarker = "/" + EncoderPdf.MARKER + new String(HiUtils.toStringHexRaw(hash2));
        LOG.debug("keyMarker = " + keyMarker);

        ArrayList<PdfDictionary> dicts = new ArrayList<>();

        ProgressMessage msg = new ProgressMessage(ProgressStepEnum.READ, 0);
        for (int i = 0; i < 8; i++) {
            msg.setNbBitsTotal(Integer.MAX_VALUE, i);
        }
        msg.setNbBitsCapacity(Integer.MAX_VALUE);
        msg.setProgress(0.0);
        params.getProgressCallBack().update(msg);

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(in));

        int numberOfPdfObjects = pdfDoc.getNumberOfPdfObjects();
        for (int i = 0; i < numberOfPdfObjects; i++) {
            PdfObject obj = pdfDoc.getPdfObject(i);
            if ((obj != null) && (obj.isDictionary())) {
                dicts.add((PdfDictionary) obj);
            }
        }

        Map<Integer, String> parts = new HashMap<>();

        for (PdfDictionary dic : dicts) {
            Set<Map.Entry<PdfName,PdfObject>> entries = dic.entrySet();
            for (Map.Entry<PdfName,PdfObject> entry : entries) {
                String key = entry.getKey().toString();
                if (key.startsWith(keyMarker)) {
                    String idx = key.substring(keyMarker.length());
                    try {
                        int i = Integer.parseInt(idx);
                        parts.put(i, entry.getValue().toString());
                        LOG.debug("Found part " + i);
                    } catch (NumberFormatException e) {
                        // Ignore
                        LOG.warn("MARKER can not be parsed : " + idx);
                    }
                }
            }
        }
        pdfDoc.close();

        msg.setStep(ProgressStepEnum.DECODE);
        msg.setProgress(0.4);
        params.getProgressCallBack().update(msg);

        if (parts.isEmpty()) {
            LOG.debug("No data");
            throw new NoBagException();
        }
        WalkerPdf w = new WalkerPdf(hash, parts.size());

        // Rebuild Base64 secret
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (!w.inc()) {
                throw new TruncatedBagException();
            }
            int idx = w.getIdxCur();
            String part = parts.get(idx);
            if (part == null) {
                LOG.error("part " + idx + " not found");
                throw new HiDataDecoderException("Part not found");
            }
            LOG.debug("appending part " + idx);
            encoded.append(part);

        }

        msg.setProgress(0.8);
        params.getProgressCallBack().update(msg);

        byte[] secret = Base64.getDecoder().decode(encoded.toString());

        msg.setProgress(1.0);
        msg.setNbBitsUsed(secret.length*8);
        params.getProgressCallBack().update(msg);

        out.write(secret);
    }
}
