package com.secretlib.io.stream;

import com.secretlib.exception.HiDataEncodeSpaceException;
import com.secretlib.exception.HiDataEncoderException;
import com.secretlib.io.pdf.EncoderPdf;
import com.secretlib.io.png.EncoderPng;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HiDataPdfOutputStream extends HiDataAbstractOutputStream {

    private static final Log LOG = new Log(HiDataPdfOutputStream.class);

    // package-level
    static final List<String> EXTENTIONS = new ArrayList<>();

    private InputStream in;

    static {
        EXTENTIONS.add("pdf");
    }

    public HiDataPdfOutputStream() {

    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(EXTENTIONS);
    }

    public boolean matches(String ext) {
        return EXTENTIONS.indexOf(ext) >= 0;
    }

    public HiDataAbstractOutputStream create(InputStream in, OutputStream out, Parameters p) {
        return new HiDataPdfOutputStream(in, out, p);
    }

    /**
     *  /!\ Providing an already-secret-encoded JPEG image
     *      as an input carrier image to generate a PNG output image
     *      will destroy the original JPEG-encoded secret data.
     *
     * @param in the carrier image of any file format
     * @param out the PNG containing the secret data
     * @param p the secret encoding params
     *
     */
    protected HiDataPdfOutputStream(InputStream in, OutputStream out, Parameters p) {
        super(out, p);
        LOG.begin("HiDataPdfOutputStream");
        this.in = in;
        LOG.end("HiDataPdfOutputStream");
    }

    /**
     * Actually generate the output image
     *
     * @throws IOException if any I/O error occurs or no JPEG frame was loaded
     * @throws HiDataEncoderException if any JPEG-related compression error occurs
     * @throws HiDataEncodeSpaceException if the secret exceeds the carrier image's storage capacity
     */
    @Override
    public void close() throws IOException, HiDataEncoderException, HiDataEncodeSpaceException {
        LOG.begin("close");
        try {
            EncoderPdf encoder = new EncoderPdf();
            if (!encoder.encode(in, out, buf.toByteArray(), params)) {
                throw new HiDataEncodeSpaceException();
            }
        } catch (Exception e) {
            throw new HiDataEncoderException(e);
        }
        finally {
            LOG.end("close");
        }
        super.close();
        LOG.end("close");
    }
}
