package com.secretlib.io.stream;

import com.secretlib.exception.HiDataEncodeSpaceException;
import com.secretlib.exception.HiDataEncoderException;
import com.secretlib.io.jpeg.EncoderJpeg;
import com.secretlib.io.jpeg.Frame;
import com.secretlib.io.jpeg.ReaderJpeg;
import com.secretlib.io.jpeg.WriterJpeg;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;
import com.secretlib.util.HiUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Florent FRADET
 * <p>
 * OutputStream to write a JPEG steganographic image
 */
public class HiDataJpegOutputStream extends HiDataAbstractOutputStream {
    private static final Log LOG = new Log(HiDataJpegOutputStream.class);

    // package-level
    static final List<String> EXTENTIONS = new ArrayList<>();

    private Frame frame = null;

    static {
        EXTENTIONS.add("jpg");
        EXTENTIONS.add("jpe");
        EXTENTIONS.add("jpeg");
        EXTENTIONS.add("jfif");
    }

    public HiDataJpegOutputStream() {

    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(EXTENTIONS);
    }

    public boolean matches(String ext) {
        return EXTENTIONS.indexOf(ext) >= 0;
    }

    public HiDataAbstractOutputStream create(InputStream in, OutputStream out, Parameters p) throws IOException {
        return new HiDataJpegOutputStream(in, out, p);
    }


    /**
     * /!\ Providing an already-secret-encoded PNG image
     * as an input carrier image to generate a JPEG output image
     * will destroy the original PNG-encoded secret data.
     *
     * @param in  the carrier image of any file format
     * @param out the JPEG containing the secret data
     * @param p   the secret encoding params
     * @throws IOException if the carrier image could not be read
     */
    protected HiDataJpegOutputStream(InputStream in, OutputStream out, Parameters p) throws IOException {
        super(out, p);
        LOG.begin("HiDataJpegOutputStream");
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(HiUtils.readAllBytes(in));
        in.close();
        ReaderJpeg reader = null;
        try {
            reader = new ReaderJpeg(data.toByteArray(), params);
            reader.decode();
        } catch (Exception e) {
            try {
                LOG.warn("Carrier image is being rewrited in-memory");
                // Read the original JPG
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data.toByteArray()));
                if (img == null) {
                    throw new IOException("Unable to read image carrier before rewriting in-memory");
                }
                // Clear the alpha channel
                BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = copy.createGraphics();
                g2d.setColor(Color.BLACK); // Or what ever fill color you want...
                g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
                g2d.drawImage(img, 0, 0, null);
                g2d.dispose();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                // Write it as a clean image since JPG can hold multiple progressive scans
                // Progressive scans are not easy to deal with
                ImageIO.write(copy, "jpg", bos);

                reader = new ReaderJpeg(bos.toByteArray(), params);
                reader.decode();
            } catch (Exception e2) {
                LOG.error("Unable to decode the JPEG carrier image");
                throw new IOException("Unable to decode the JPEG carrier image");
            }
        }
        frame = reader.getFrame();
        LOG.end("HiDataJpegOutputStream");
    }

    /**
     * Actually generate the output image
     *
     * @throws IOException                if any I/O error occurs or no JPEG frame was loaded
     * @throws HiDataEncoderException     if any JPEG-related compression error occurs
     * @throws HiDataEncodeSpaceException if the secret exceeds the carrier image's storage capacity
     */
    @Override
    public void close() throws IOException, HiDataEncoderException, HiDataEncodeSpaceException {
        LOG.begin("close");
        if (frame != null) {
            try {
                EncoderJpeg encoder = new EncoderJpeg();
                boolean b = encoder.encode(frame, buf.toByteArray(), params);
                if (!b) {
                    LOG.error("Encoding interrupted : secret too large");
                    throw new HiDataEncodeSpaceException();
                }
                WriterJpeg writer = new WriterJpeg(frame, out, params);
                writer.write();
            } catch (Exception e) {
                LOG.error(e.getMessage());
                throw new HiDataEncoderException(e);
            } finally {
                out.close();
                LOG.end("close");
            }
        } else {
            out.close();
            LOG.error("No JPG frame loaded");
            throw new IOException("HiDataJpegOutputStream.close() : No JPG frame loaded");
        }
        LOG.end("close");
    }
}