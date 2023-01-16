package com.secretlib.io.pdf;

import com.itextpdf.kernel.pdf.*;
import com.secretlib.exception.HiDataEncodeSpaceException;
import com.secretlib.io.png.EncoderPng;
import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

/**
 * @author Florent FRADET
 */
public class EncoderPdf {

    private static final Log LOG = new Log(EncoderPdf.class);

    static final String MARKER = "T-S.D";
    static final PdfName MARKER_PDF = new PdfName(MARKER);

    protected void markFile(InputStream in, OutputStream out) throws Exception {
        // Suppress unused objects
        // Mark usable points to identify persistent ones
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(in), new PdfWriter(out));
        int numberOfPdfObjects = pdfDoc.getNumberOfPdfObjects();
        for (int i = 0; i < numberOfPdfObjects; i++) {
            PdfObject obj = pdfDoc.getPdfObject(i);
            if ((obj != null) && (obj.isDictionary())) {
                PdfDictionary dic = (PdfDictionary) obj;
                dic.put(MARKER_PDF, new PdfString("Dic" + i));
                LOG.debug("Mark dic " + i);
            }
        }

        pdfDoc.close();
    }


    public boolean encode(InputStream in, OutputStream out, byte[] data, Parameters params) throws Exception {

        ProgressMessage msg = new ProgressMessage(ProgressStepEnum.READ, 0);
        for (int i = 0; i < 8; i++) {
            msg.setNbBitsTotal(Integer.MAX_VALUE, i);
        }
        msg.setNbBitsCapacity(Integer.MAX_VALUE);
        msg.setProgress(0.0);
        params.getProgressCallBack().update(msg);

        byte[] pass = params.getKm().getBytes(StandardCharsets.UTF_8);
        byte[] hash = HiUtils.genHash(pass, params.getHashAlgo());
        byte[] hash2 = HiUtils.genHash(hash, params.getHashAlgo());
        String keyMarker = EncoderPdf.MARKER + new String(HiUtils.toStringHexRaw(hash2));
        LOG.debug("keyMarker = " + keyMarker);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        markFile(in, os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(is), new PdfWriter(out));

        ArrayList<PdfDictionary> dicts = new ArrayList<>();

        int numberOfPdfObjects = pdfDoc.getNumberOfPdfObjects();
        for (int i = 0; i < numberOfPdfObjects; i++) {
            PdfObject obj = pdfDoc.getPdfObject(i);
            if ((obj != null) && (obj.isDictionary())) {
                PdfDictionary dic = (PdfDictionary)obj;
                PdfObject val = dic.get(MARKER_PDF);
                if ((val != null) && (val instanceof PdfString)) {
                    dic.remove(MARKER_PDF);
                    LOG.debug("Using " + val.toString());
                    dicts.add((PdfDictionary) obj);
                }
            }
        }

        // Convert byte data into base64 strings
        String encoded = Base64.getEncoder().encodeToString(data);

        msg.setStep(ProgressStepEnum.ENCODE);
        msg.setProgress(0.4);
        params.getProgressCallBack().update(msg);

        // Split data into parts
        int nbParts = dicts.size();
        String[] parts = new String[nbParts];
        Arrays.fill(parts, null);

        int partSize = (encoded.length() / nbParts) + 1;
        int idx = 0;
        int i;
        for (i = 0; i < nbParts; i++) {
            if (idx >= encoded.length()) {
                // More parts than encoded size :
                // Later parts remain empty.
                break;
            }
            int idxEnd = idx + partSize;
            if (idxEnd > encoded.length()) {
                idxEnd = encoded.length();
            }
            parts[i] = encoded.substring(idx, idxEnd);
            idx = idxEnd;
        }

        int nbUsedParts = i;
        WalkerPdf w = new WalkerPdf(hash, nbUsedParts);

        msg.setProgress(0.8);
        params.getProgressCallBack().update(msg);

        // Put parts into persistent PdfDictionaries
        for (i = 0; i < nbUsedParts; i++) {
            if (!w.inc()) {
                pdfDoc.close();
                // No space enough
                return false;
            }
            idx = w.getIdxCur();
            PdfDictionary dic = dicts.get(idx);
            String part = parts[i];
            dic.put(new PdfName(keyMarker + idx), new PdfString(part));
            LOG.debug("Add part " + i + " @ idx " + idx);

        }
        msg.setStep(ProgressStepEnum.WRITE);
        params.getProgressCallBack().update(msg);

        pdfDoc.close();

        msg.setProgress(1.0);
        msg.setNbBitsUsed(encoded.length()*8);
        params.getProgressCallBack().update(msg);

        return true;
    }
}
