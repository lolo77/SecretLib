package com.secretlib.util;

import com.secretlib.model.IProgressCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author Florent FRADET
 *
 */
public class Parameters {

    private static final Log LOG = new Log(Parameters.class);


    private static final String CODEC = "codec";
    private String codec = null;

    private static final String HASH_ALGO = "h";
    private String hashAlgo = "SHA-512";

    private static final String KEY_MASTER = "km";
    private String km = "";

    private static final String KEY_DATA = "kd";
    private String kd = "";

    private static final String BIT_START = "bs";
    private int bitStart = 0;

    private static final String AUTO_EXTEND_BIT = "eb";
    private boolean autoExtendBit = false;

    private HashMap<String, Object> extendedParams = new HashMap<>();


    private IProgressCallback progressCallBack = null;

    public Parameters () {
        LOG.begin("constructor");

        LOG.end("constructor");
    }

    public Parameters (Parameters p) {
        this();
        LOG.begin("constructor copy");

        codec = p.codec;
        hashAlgo = p.hashAlgo;
        km = p.km;
        kd = p.kd;
        bitStart = p.bitStart;
        autoExtendBit = p.autoExtendBit;
        extendedParams = p.extendedParams;
        progressCallBack = p.progressCallBack;

        LOG.end("constructor copy");
    }

    public Parameters (String[] args) {
        this();
        LOG.begin("constructor String[] args");
        parse(args);
        LOG.end("constructor String[] args");
    }

    protected void parse(String[] args) {
        Iterator<String> iter = Arrays.stream(args).iterator();
        while (iter.hasNext()) {
            consumeArg(iter);
        }
    }

    protected boolean consumeArg(Iterator<String> iter) {

        LOG.begin("consumeArg");

        boolean b = false;
        String arg = iter.next();

        LOG.debug("arg : " + arg);


        if (CODEC.equals(arg)) {
            codec = iter.next();
            b = true;
        }

        if (HASH_ALGO.equals(arg)) {
            hashAlgo = iter.next();
            b = true;
        }

        if (KEY_MASTER.equals(arg)) {
            km = iter.next();
            b = true;
        }

        if (KEY_DATA.equals(arg)) {
            kd = iter.next();
            b = true;
        }

        if (BIT_START.equals(arg)) {
            b = true;
            try {
                int v = Integer.parseInt(iter.next());
                if ((v < 0) || (v > 7)) {
                    throw new NumberFormatException();
                }
                bitStart = v;
            }
            catch (NumberFormatException e) {
                LOG.error("'bs' option ignored - Must be followed by an int [0;7]");
            }
        }

        if (AUTO_EXTEND_BIT.equals(arg)) {
            autoExtendBit = true;
            b = true;
        }

        if (!b) {
            b = consumeArgExt(arg, iter);
        }

        LOG.end("consumeArg returns " + b);
        return b;
    }

    protected boolean consumeArgExt(String arg, Iterator<String> iter) {
        // To be overridden in child class
        LOG.warn("Unknown option '" + arg + "' - Ignored");
        return false;
    }

    public String getCodec() {
        return codec;
    }
    public String getHashAlgo() {
        return hashAlgo;
    }

    public String getKm() {
        return km;
    }

    public String getKd() {
        return kd;
    }

    public int getBitStart() {
        return bitStart;
    }

    public boolean isAutoExtendBit() {
        return autoExtendBit;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public void setHashAlgo(String hashAlgo) {
        this.hashAlgo = hashAlgo;
    }

    public void setKm(String km) {
        this.km = km;
    }

    public void setKd(String kd) {
        this.kd = kd;
    }

    public void setBitStart(int bitStart) {
        this.bitStart = bitStart;
    }

    public void setAutoExtendBit(boolean autoExtendBit) {
        this.autoExtendBit = autoExtendBit;
    }

    public IProgressCallback getProgressCallBack() {
        return progressCallBack;
    }

    public void setProgressCallBack(IProgressCallback progressCallBack) {
        this.progressCallBack = progressCallBack;
    }

    public HashMap<String, Object> getExtendedParams() {
        return extendedParams;
    }
}
