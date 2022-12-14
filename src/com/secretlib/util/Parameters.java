package com.secretlib.util;

import com.secretlib.model.IProgressCallback;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Florent FRADET
 *
 */
public class Parameters {

    private static final Log LOG = new Log(Parameters.class);

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

    private IProgressCallback progressCallBack = null;

    public Parameters () {
        LOG.begin("constructor");

        LOG.end("constructor");
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
            consumeArgExt(arg, iter);
        }

        LOG.end("consumeArg returns " + b);
        return b;
    }

    protected void consumeArgExt(String arg, Iterator<String> iter) {
        // To be overridden in child class
        LOG.warn("Unknown option '" + arg + "' - Ignored");
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
}
