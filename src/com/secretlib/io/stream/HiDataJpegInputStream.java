package com.secretlib.io.stream;

import com.secretlib.exception.BagParseFinishException;
import com.secretlib.exception.HiDataDecoderException;
import com.secretlib.io.jpeg.DecoderJpeg;
import com.secretlib.io.jpeg.ReaderJpeg;
import com.secretlib.model.HiDataBagBuilder;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;
import com.secretlib.util.HiUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author Florent FRADET
 *
 * InputStream decoder for JPEG steganographic image
 */
public class HiDataJpegInputStream extends HiDataAbstractInputStream {
    private static final Log LOG = new Log(HiDataJpegInputStream.class);

    static final String CODEC_NAME = "JPEG/BINARY/20230101";

    private static final byte[] HEADER = new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF};

    public HiDataJpegInputStream() {

    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(HiDataJpegOutputStream.EXTENTIONS);
    }

    public boolean matches(byte[] in) {
        return (HiUtils.arraysEquals(HEADER, 0, in, 0, HEADER.length));
    }


    public HiDataAbstractInputStream create(InputStream in, Parameters p) throws IOException {
        return new HiDataJpegInputStream(in, p);
    }

    @Override
    public String getCodecName() {
        return CODEC_NAME;
    }


    /**
     *
     * @param in the jpg/jpeg image to decode
     * @param p the secret decoding params
     * @throws IOException if the file is corrupted or any other I/O error
     * @throws HiDataDecoderException if there is no readable secret data
     */
    public HiDataJpegInputStream(InputStream in, Parameters p) throws IOException {
        super();

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(HiUtils.readAllBytes(in));
        in.close();
        ReaderJpeg reader = null;
        try {
            reader = new ReaderJpeg(data.toByteArray(), p);
            reader.decode();
        } catch (Exception e) {
            try {
                // Since this part is not mandatory in reading/decoding a jpeg,
                // the rewrite process is performed to send the correct byte storage capacity
                // to the message callback
                LOG.warn("Carrier image is being rewrited in-memory");
                // Read the original JPG
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data.toByteArray()));
                if (img == null) {
                    throw new IOException("Unable to read image carrier before rewriting in-memory");
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                // Write it as a clean image since JPG can hold multiple progressive scans
                // Progressive scans are not easy to deal with
                ImageIO.write(img, "jpg", bos);

                reader = new ReaderJpeg(bos.toByteArray(), p);
                reader.decode();
            } catch (Exception e2) {
                LOG.error("Unable to decode the JPEG carrier image");
                throw new IOException("Unable to decode the JPEG carrier image");
            }
        }
        HiDataBagBuilder bagBuilder = new HiDataBagBuilder(getBag());
        try {
            DecoderJpeg.decode(reader.getFrame(), p, bagBuilder);
        } catch (BagParseFinishException e) {
            // NO OP
        }
        bagBuilder.close();
    }
}
