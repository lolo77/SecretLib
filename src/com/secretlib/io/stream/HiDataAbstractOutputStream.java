package com.secretlib.io.stream;

import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @Author : Florent FRADET
 *
 * Writes the secret-encoded image to the specified OutputStream
 *
 * Call this.write(...) to encode the secret data
 * Call this.close() to write the output steganographic image (constructor's OutputStream)
 */
public abstract class HiDataAbstractOutputStream extends OutputStream {
    private static final Log LOG = new Log(HiDataAbstractOutputStream.class);


    protected ByteArrayOutputStream buf;
    protected Parameters params;
    protected OutputStream out;

    public abstract List<String> getExtensions();
    public abstract boolean matches(String ext);
    public abstract HiDataAbstractOutputStream create(InputStream in, OutputStream out, Parameters p) throws IOException;

    public HiDataAbstractOutputStream() {

    }

    /**
     *
     * @param out the generated steganographic image
     * @param p the encoding params
     */
    protected HiDataAbstractOutputStream(OutputStream out, Parameters p) {
        this.out = out;
        params = p;
        buf = new ByteArrayOutputStream();
    }


    @Override
    public void write(int i) throws IOException {
        buf.write(i);
    }

}
