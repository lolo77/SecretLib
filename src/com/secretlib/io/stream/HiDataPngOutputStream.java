package com.secretlib.io.stream;

import com.secretlib.exception.HiDataEncodeSpaceException;
import com.secretlib.io.png.EncoderPng;
import com.secretlib.exception.HiDataEncoderException;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author : Florent FRADET
 *
 * OutputStream to write a PNG steganographic image
 */
public class HiDataPngOutputStream extends HiDataAbstractOutputStream {

    private static final Log LOG = new Log(HiDataPngOutputStream.class);

    static final String CODEC_NAME = "SecretLib/1/PNG";

    // package-level
    static final List<String> EXTENTIONS = new ArrayList<>();

    private InputStream in;

    static {
        EXTENTIONS.add("png");
    }

    public HiDataPngOutputStream() {

    }

    @Override
    public String getCodecName() {
        return CODEC_NAME;
    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(EXTENTIONS);
    }

    public boolean matches(String ext) {
        return EXTENTIONS.indexOf(ext) >= 0;
    }

    public HiDataAbstractOutputStream create(InputStream in, OutputStream out, Parameters p) {
        return new HiDataPngOutputStream(in, out, p);
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
    protected HiDataPngOutputStream(InputStream in, OutputStream out, Parameters p) {
        super(out, p);
        LOG.begin("HiDataPngOutputStream");
        this.in = in;
        LOG.end("HiDataPngOutputStream");
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

        BufferedImage imgCarrier = ImageIO.read(in);
        in.close();
        if (imgCarrier == null) {
            throw new IOException("Unable to read carrier before encoding");
        }
        try {
            EncoderPng encoder = new EncoderPng();
            if (!encoder.encode(imgCarrier, buf.toByteArray(), params)) {
                throw new HiDataEncodeSpaceException();
            }
        } catch (Exception e) {
            throw new HiDataEncoderException(e);
        }
        finally {
            LOG.end("close");
        }

        try {
            ImageIO.write(imgCarrier, "png", out);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            throw e;
        }
        finally {
            out.close();
            LOG.end("close");
        }
        super.close();
        LOG.end("close");
    }
}
