package com.secretlib.io.stream;

import com.secretlib.exception.BagParseFinishException;
import com.secretlib.io.png.DecoderPng;
import com.secretlib.exception.HiDataDecoderException;
import com.secretlib.model.HiDataBagBuilder;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;
import com.secretlib.util.HiUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * @Author : Florent FRADET
 *
 * InputStream decoder for PNG steganographic image
 */
public class HiDataPngInputStream extends HiDataAbstractInputStream {

    private static final Log LOG = new Log(HiDataPngInputStream.class);

    private static final byte[] HEADER = new byte[] {(byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A};

    static final String CODEC_NAME = "PNG/BINARY/20230101";


    public HiDataPngInputStream()
    {

    }

    @Override
    public String getCodecName() {
        return CODEC_NAME;
    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(HiDataPngOutputStream.EXTENTIONS);
    }

    public boolean matches(byte[] in) {
        return (HiUtils.arraysEquals(HEADER, 0, in, 0, HEADER.length));
    }


    public HiDataAbstractInputStream create(InputStream in, Parameters p) throws IOException {
        return new HiDataPngInputStream(in, p);
    }

    /**
     *
     * @param in the png image to decode
     * @param p the secret decoding params
     * @throws IOException if the file is corrupted or any other I/O error
     * @throws HiDataDecoderException if there is no readable secret data
     */
    public HiDataPngInputStream(InputStream in, Parameters p) throws IOException {
        super();
        BufferedImage img = ImageIO.read(in);
        in.close();
        HiDataBagBuilder bagBuilder = new HiDataBagBuilder(getBag());
        try {
            DecoderPng.decode(img, p, bagBuilder);
        } catch (BagParseFinishException e) {
            // NO OP
        }
        bagBuilder.close();

    }
}
