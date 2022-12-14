package com.secretlib;

import com.secretlib.job.HiDataJob;
import com.secretlib.util.BatchParameters;
import com.secretlib.util.Log;

import javax.imageio.ImageIO;

/**
 * @author Florent FRADET
 *
 * HiData - "Hide Data" using images using steganography
 *
 */
public class Main {

    private static final Log LOG = new Log(Main.class);

    public static void main(String[] args) {
        LOG.begin("main");

        ImageIO.setUseCache(false);

        try {
            BatchParameters p = new BatchParameters(args);
            HiDataJob job = HiDataJob.create(p);
            job.run();
        } catch (Exception e) {
            LOG.error(e.getClass().toString() + " : " + e.getMessage());
        }

        LOG.end("main");
    }
}
