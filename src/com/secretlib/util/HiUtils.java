package com.secretlib.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Florent FRADET
 */
public class HiUtils {
    private static final Log LOG = new Log(HiUtils.class);

    public static byte[] digest(byte[] input, String algo) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        return md.digest(input);
    }

    public static String toStringHexRaw(byte[] data) {
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            return "null";
        }

        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X", (int)(data[i]) & 0xFF));
        }
        return sb.toString();
    }

    public static String toStringHex(byte[] data, int limit) {
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            return "null";
        }
        if ((limit <= 0) || (limit > data.length)) {
            limit = data.length;
        }
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", (int)(data[i]) & 0xFF));
        }
        if (limit < data.length) {
            sb.append("... (" + data.length + " bytes)");
        }
        return sb.toString();
    }

    public static String intToBin(int v) {
        StringBuilder sb = new StringBuilder();
        int b = 1 << 15;
        int i = 0;
        while (b != 0) {
            sb.append(((v & b) > 0) ? "1" : "0");
            b >>= 1;
            i++;
            if ((b != 0) && ((i & 3) == 0)) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static byte[] genHash(byte[] data, String algo) {
        if (data == null) {
            return new byte[0];
        }

        byte[] hash = HiUtils.digest(data, algo);
        return hash;
    }

    public static String getFileExt(File f) {
        LOG.begin("getFileExt");
        String s = null;
        if ((f != null) && (!f.isDirectory())) {
            String sName = f.getName();
            int i = sName.lastIndexOf(".");
            if (i >= 0) {
                s = sName.substring(i + 1);
            }
        }
        LOG.end("getFileExt returns " + s);
        return s;
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int iRead;
        while ((iRead = in.read(tmp)) > 0) {
            buf.write(tmp, 0, iRead);
        }
        return buf.toByteArray();
    }

    public static boolean arraysEquals(byte[] a, int ofsa, byte[] b, int ofsb, int len) {

        if (ofsa + len > a.length)
            return false;

        if (ofsb + len > b.length)
            return false;

        int ia = ofsa;
        int ib = ofsb;
        for (int i = 0; i < len; i++, ia++, ib++) {
            if (a[ia] != b[ib])
                return false;
        }

        return true;
    }
}
