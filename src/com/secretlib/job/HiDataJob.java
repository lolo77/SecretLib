package com.secretlib.job;

import com.secretlib.util.BatchParameters;
import com.secretlib.util.Log;

/**
 * @author Florent FRADET
 */
public abstract class HiDataJob {

    private static final Log LOG = new Log(HiDataJob.class);

    protected BatchParameters params = null;

    public abstract void run() throws Exception;


    protected HiDataJob(BatchParameters p) {
        this.params = p;
    }

    public static HiDataJob create(BatchParameters p) {
        LOG.begin("create");

        HiDataJob job = null;
        try {

            if (p.isDecode()) {
                job = new HiDataDecoderJob(p);
            } else {
                job = new HiDataEncoderJob(p);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        LOG.end("create");
        return job;
    }



}
