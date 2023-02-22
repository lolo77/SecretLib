package com.secretlib.io.stream;

import com.secretlib.util.Log;
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

    private static final Log LOG = new Log(HiDataStreamFactory.class);

    // Stream lists
    private static final List<HiDataAbstractInputStream> LST_IN = new ArrayList<>();
    private static final List<HiDataAbstractOutputStream> LST_OUT = new ArrayList<>();

    // Codec maps
    private static Map<String, HiDataAbstractInputStream> MAP_IN = new HashMap<>();
    private static Map<String, HiDataAbstractOutputStream> MAP_OUT = new HashMap<>();

    // Extension lists
    private static List<String> lstExtIn;
    private static List<String> lstExtOut;



    /**
     * Register default HiData streams
     */
    static {
        registerInputStream(new HiDataPngInputStream());
        registerOutputStream(new HiDataPngOutputStream());

        registerInputStream(new HiDataJpegInputStream());
        registerOutputStream(new HiDataJpegOutputStream());

        try {
            ClassLoader.getSystemClassLoader().loadClass("com.itextpdf.kernel.pdf.PdfDocument");
            registerInputStream(new HiDataPdfInputStream());
            registerOutputStream(new HiDataPdfOutputStream());
        } catch (ClassNotFoundException e) {
            LOG.warn("Libraries not found : PDF support disabled");
        }
    }

    public static void registerInputStream(HiDataAbstractInputStream stream) {
        LST_IN.add(stream);
        MAP_IN.put(stream.getCodecName(), stream);
        updateListSupportedInputExtensions();
    }

    public static void registerOutputStream(HiDataAbstractOutputStream stream) {
        LST_OUT.add(stream);
        MAP_OUT.put(stream.getCodecName(), stream);
        updateListSupportedOutputExtensions();
    }

    public static void unregisterInputStream(HiDataAbstractInputStream stream) {
        if (LST_IN.remove(stream)) {
            MAP_IN.remove(stream.getCodecName());
            updateListSupportedInputExtensions();
        }
    }

    public static void unregisterOutputStream(HiDataAbstractOutputStream stream) {
        if (LST_OUT.remove(stream)) {
            MAP_OUT.remove(stream.getCodecName());
            updateListSupportedInputExtensions();
        }
    }

    public static boolean isSupportedInputExtension(String ext) {
        return lstExtIn.contains(ext);
    }

    public static boolean isSupportedOutputExtension(String ext) {
        return lstExtOut.contains(ext);
    }

    private static void updateListSupportedInputExtensions() {
        List<String> lst = new ArrayList<>();
        for (HiDataAbstractInputStream is : LST_IN) {
            for (String ext : is.getExtensions()) {
                if (!lst.contains(ext)) {
                    lst.add(ext);
                }
            }
        }
        lstExtIn = lst;
    }

    private static void updateListSupportedOutputExtensions() {
        List<String> lst = new ArrayList<>();
        for (HiDataAbstractOutputStream is : LST_OUT) {
            for (String ext : is.getExtensions()) {
                if (!lst.contains(ext)) {
                    lst.add(ext);
                }
            }
        }
        lstExtOut = lst;
    }

    public static List<String> getSupportedInputExtensions() {
        return Collections.unmodifiableList(lstExtIn);
    }

    public static List<String> getSupportedOutputExtensions() {
        return Collections.unmodifiableList(lstExtOut);
    }

    public static Set<String> getListCodecsInput() {
        return Collections.unmodifiableSet(MAP_IN.keySet());
    }

    public static Set<String> getListCodecsOutput() {
        return Collections.unmodifiableSet(MAP_OUT.keySet());
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

        String codec = p.getCodec();
        for (HiDataAbstractInputStream inst : LST_IN) {
            if (inst.matches(data)) {
                if ((codec == null) || (inst.getCodecName().equals(codec))) {
                    return inst.create(new ByteArrayInputStream(data), p);
                }
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
        String codec = p.getCodec();
        for (HiDataAbstractOutputStream inst : LST_OUT) {
            if (inst.matches(lcExt)) {
                if ((codec == null) || (inst.getCodecName().equals(codec))) {
                    return inst.create(in, out, p);
                }
            }
        }

        return null;
    }

}
