package com.secretlib.job;

import com.secretlib.exception.TruncatedBagException;
import com.secretlib.io.stream.HiDataAbstractInputStream;
import com.secretlib.io.stream.HiDataStreamFactory;
import com.secretlib.model.*;
import com.secretlib.util.BatchParameters;
import com.secretlib.util.Log;

import java.io.*;

public class HiDataDecoderJob extends HiDataJob {

    private static final Log LOG = new Log(HiDataDecoderJob.class);

    public HiDataDecoderJob(BatchParameters p) {
        super(p);
    }



    private void writeFrags(HiDataAbstractInputStream ins) throws IOException, TruncatedBagException {
        String sOutputFile = params.getFileNameDataOutput();
        File fOutput = new File(sOutputFile);
        int chunkNum = 0;
        HiDataBag bag = ins.getBag();
        if (ins.getBag() == null) {
            return;
        }
        for (AbstractChunk d : bag.getItems()) {
            if (EChunk.DATA.equals(d.getType())) {
                ChunkData dfd = (ChunkData)d;
                try {
                    dfd.decryptData(params);
                    String sFileName = dfd.getName();
                    if ((sFileName == null) || (sFileName.length() == 0)) {
                        sFileName = sOutputFile;
                        if ((!sOutputFile.endsWith("/")) && (!sOutputFile.endsWith("\\"))) {
                            sFileName += "/";
                        }
                        sFileName += "item" + chunkNum;
                        chunkNum++;
                    }
                    LOG.debug("Extracting file " + sFileName);
                    File f = new File(fOutput.getAbsolutePath() + "/" + sFileName);
                    FileOutputStream fos = new FileOutputStream(f);
                    dfd.write(fos);
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("Data extraction error : " + e.getMessage());
                }
            }
        }
    }


    private void checkParams() throws Exception {
        if (params.getFileNameImgCarrier() == null) {
            throw new Exception("'c' param missing : Carrier image file (where data is hidden)");
        }
    }


    public void run() throws Exception {
        LOG.begin("run");
        checkParams();

        FileInputStream fis = new FileInputStream(new File(params.getFileNameImgCarrier()));
        HiDataAbstractInputStream ins = HiDataStreamFactory.createInputStream(fis, params);
        writeFrags(ins);

        LOG.end("run");
    }
}
