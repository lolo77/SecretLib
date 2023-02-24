package com.secretlib.job;

import com.secretlib.io.stream.*;
import com.secretlib.model.HiDataBag;
import com.secretlib.model.ChunkData;
import com.secretlib.util.BatchParameters;
import com.secretlib.util.Log;
import com.secretlib.util.HiUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Florent FRADET
 */
public class HiDataEncoderJob extends HiDataJob {

    private static final Log LOG = new Log(HiDataEncoderJob.class);

    public HiDataEncoderJob(BatchParameters p) {
        super(p);
    }


    private void loadInputFile(HiDataBag bag) {
        LOG.begin("loadInputFile");

        File fSecret = new File(params.getFileNameDataInput());

        ChunkData data = new ChunkData();
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(fSecret));
            data.setData(HiUtils.readAllBytes(bis));

            if (params.getFileInputDisplayName() != null) {
                if (BatchParameters.FILE_INPUT_DISPLAY_NAME_SAME.equals(params.getFileInputDisplayName())) {
                    data.setName(fSecret.getName());
                } else {
                    data.setName(params.getFileInputDisplayName());
                }
            }
            // Creates the rawData
            data.encryptData(params);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LOG.crit(e.getMessage());
            LOG.end("loadInputFile");
            return;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            LOG.crit(e.getMessage());
            LOG.end("loadInputFile");
            return;
        }
        finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    LOG.crit(e.getMessage());
                }
            }
        }

        bag.addItem(data);

        LOG.debug(bag.toString());

        LOG.end("loadInputFile");
    }

    private void checkParams() throws Exception {
        if (params.getFileNameDataInput() == null) {
            throw new Exception("'i' param missing : Input file (data to hide)");
        }

        if (params.getFileNameImgCarrier() == null) {
            throw new Exception("'c' param missing : Image carrier (data holder)");
        }

        if (params.getFileNameImgCarrier().equals(params.getFileNameDataOutput())) {
            throw new Exception("'o' param error : output file must differ from input carrier file");
        }
    }


    public void run() throws Exception {
        LOG.begin("run");

        checkParams();
        File fIn = new File(params.getFileNameImgCarrier());
        FileInputStream fsIn = new FileInputStream(fIn);

        File fOut = new File(params.getFileNameDataOutput());
        FileOutputStream fsOut = new FileOutputStream(fOut);

        HiDataBag bag = new HiDataBag();
        if (params.isAppend()) {
            HiDataAbstractInputStream ins = HiDataStreamFactory.createInputStream(fsIn, params);
            params.setCodec(ins.getOutputCodecName()); // Select the reciprocal codec
            bag = ins.getBag();
            fsIn = new FileInputStream(fIn); // Reset stream
        }

        try {
            HiDataAbstractOutputStream out = HiDataStreamFactory.createOutputStream(fsIn, fsOut, params);
            loadInputFile(bag);
            out.write(bag.toByteArray());
            // Encoding is actually done on close() call
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.crit("Could not write output file. Check the file extension.");
        }

        LOG.end("run");
    }
}
