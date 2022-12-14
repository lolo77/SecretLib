package com.secretlib.io.stream;

import com.secretlib.util.Parameters;
import com.secretlib.util.HiUtils;

import java.io.*;
import java.util.*;


/**
 * @author Florent FRADET
 *
 * Build the correct stream according to the provided InputStream file format
 *
 */
public class HiDataStreamFactory {

    private static final List<HiDataAbstractInputStream> LST_IN = new ArrayList<>();
    private static final List<HiDataAbstractOutputStream> LST_OUT = new ArrayList<>();

    /**
     * Register default HiData streams
     */
    static {
        registerInputStream(new HiDataPngInputStream());
        registerInputStream(new HiDataJpegInputStream());

        registerOutputStream(new HiDataPngOutputStream());
        registerOutputStream(new HiDataJpegOutputStream());
    }

    public static void registerInputStream(HiDataAbstractInputStream stream) {
        LST_IN.add(stream);
    }

    public static void registerOutputStream(HiDataAbstractOutputStream stream) {
        LST_OUT.add(stream);
    }

    public static List<String> getSupportedInputExtensions() {
        List<String> lst = new ArrayList<>();
        for (HiDataAbstractInputStream is : LST_IN) {
            lst.addAll(is.getExtensions());
        }

        return lst;
    }

    public static List<String> getSupportedOutputExtensions() {
        List<String> lst = new ArrayList<>();
        for (HiDataAbstractOutputStream is : LST_OUT) {
            lst.addAll(is.getExtensions());
        }

        return lst;
    }

    /**
     *
     * @param in the input image byte stream
     * @param p the secret decoding params
     * @return the InputStream able to read and decode the provided raw bytes InputStream
     *          null if format not supported
     * @throws Exception if any I/O or decoding error occurs
     */
    public static HiDataAbstractInputStream createInputStream(InputStream in, Parameters p) throws Exception {
        byte[] data = HiUtils.readAllBytes(in);

        for (HiDataAbstractInputStream inst : LST_IN) {
            if (inst.matches(data)) {
                return inst.create(new ByteArrayInputStream(data), p);
            }
        }

        return null;
    }


    /**
     * Create the appropriate encoder OutputStream according to the provided file extension
     * Further calls to write will store the secret raw data.
     * Call to close() wille generate the output image.
     *
     * @param in the input image (either png or jpg)
     * @param out the ouput image (ie. 'ext' param) - must be a different file than the InputStream
     * @param p encoding parameters
     * @param ext the output file extension
     * @return the specific Stream
     *          null if format not supported
     * @throws Exception if any error occurs
     */
    public static HiDataAbstractOutputStream createOutputStream(InputStream in, OutputStream out, Parameters p, String ext) throws Exception {
        String lcExt = ext.toLowerCase(Locale.ROOT);
        for (HiDataAbstractOutputStream inst : LST_OUT) {
            if (inst.matches(lcExt)) {
                return inst.create(in, out, p);
            }
        }

        return null;
    }

}
