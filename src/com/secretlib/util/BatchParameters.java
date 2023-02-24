package com.secretlib.util;

import com.secretlib.model.DefaultProgressCallback;

import java.util.Iterator;

/**
 * @author Florent FRADET
 */
public class BatchParameters extends Parameters {

    private static final Log LOG = new Log(BatchParameters.class);

    private static final String LOG_LEVEL = "ll";

    private static final String IMG_CARRIER = "c";
    private String fileNameImgCarrier = null;

    private static final String FILE_INPUT = "i";
    private String fileNameDataInput = null;

    private static final String FILE_INPUT_DISPLAY_NAME = "in";
    public static final String FILE_INPUT_DISPLAY_NAME_SAME = "-";
    private String fileInputDisplayName = null;

    private static final String FILE_OUTPUT = "o";
    private String fileNameDataOutput = "output";

    private static final String APPEND = "a";
    private boolean append = false;

    private static final String DECODE = "x";
    private boolean decode = false;

    public BatchParameters() {
        LOG.begin("constructor");

        LOG.end("constructor");
    }

    public BatchParameters(Parameters p) {
        super(p);
        LOG.begin("constructor from Parameters");

        LOG.end("constructor from Parameters");
    }

    public BatchParameters (String[] args) {
        this();
        LOG.begin("constructor String[] args");
        parse(args);
        setProgressCallBack(new DefaultProgressCallback());
        LOG.end("constructor String[] args");
    }

    @Override
    protected boolean consumeArgExt(String arg, Iterator<String> iter) {
        LOG.begin("consumeArgExt");

        boolean b = false;
        if (IMG_CARRIER.equals(arg)) {
            fileNameImgCarrier = iter.next();
            b = true;
        }

        if (FILE_INPUT.equals(arg)) {
            fileNameDataInput = iter.next();
            b = true;
        }

        if (FILE_INPUT_DISPLAY_NAME.equals(arg)) {
            fileInputDisplayName = iter.next();
            b = true;
        }

        if (FILE_OUTPUT.equals(arg)) {
            fileNameDataOutput = iter.next();
            b = true;
        }

        if (LOG_LEVEL.equals(arg)) {
            b = true;
            try {
                int v = Integer.parseInt(iter.next());
                if ((v < Log.TRACE) || (v > Log.CRITICAL)) {
                    throw new NumberFormatException();
                }
                Log.setLevel(v);
            }
            catch (NumberFormatException e) {
                LOG.error("'ll' option ignored - Must be followed by an int [0;5] (TRACE/DEBUG/INFO/WARNING/ERROR/CRITICAL)");
            }
        }

        if (APPEND.equals(arg)) {
            append = true;
            b = true;
        }

        if (DECODE.equals(arg)) {
            decode = true;
            b = true;
        }

        if (!b) {
            LOG.debug("Unknown arg : " + arg);
        }

        LOG.end("consumeArgExt");
        return b;
    }


    public String getFileNameImgCarrier() {
        return fileNameImgCarrier;
    }

    public String getFileNameDataInput() {
        return fileNameDataInput;
    }

    public String getFileInputDisplayName() {
        return fileInputDisplayName;
    }

    public String getFileNameDataOutput() {
        return fileNameDataOutput;
    }

    public boolean isAppend() {
        return append;
    }

    public boolean isDecode() {
        return decode;
    }
}
