package com.secretlib.io.stream;

import com.secretlib.exception.BagParseFinishException;
import com.secretlib.exception.HiDataDecoderException;
import com.secretlib.io.pdf.DecoderPdf;
import com.secretlib.io.png.DecoderPng;
import com.secretlib.model.HiDataBagBuilder;
import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class HiDataPdfInputStream extends HiDataAbstractInputStream {

    private static final Log LOG = new Log(HiDataPdfInputStream.class);

    private static final byte[] HEADER = new byte[] {(byte)0x25, (byte)0x50, (byte)0x44, (byte)0x46, (byte)0x2D};


    public HiDataPdfInputStream()
    {

    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(HiDataPdfOutputStream.EXTENTIONS);
    }

    public boolean matches(byte[] in) {
        return (HiUtils.arraysEquals(HEADER, 0, in, 0, HEADER.length));
    }


    public HiDataAbstractInputStream create(InputStream in, Parameters p) throws IOException {
        return new HiDataPdfInputStream(in, p);
    }

    /**
     *
     * @param in the png image to decode
     * @param p the secret decoding params
     * @throws IOException if the file is corrupted or any other I/O error
     * @throws HiDataDecoderException if there is no readable secret data
     */
    public HiDataPdfInputStream(InputStream in, Parameters p) throws IOException {
        super();
        HiDataBagBuilder bagBuilder = new HiDataBagBuilder(getBag());
        try {
            DecoderPdf.decode(in, p, bagBuilder);
        } catch (BagParseFinishException e) {
            // NO OP
        }
        bagBuilder.close();
    }
}
