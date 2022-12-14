package com.secretlib.io.jpeg;

import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
    Copyright (c) 2008, Adobe Systems Incorporated
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

    * Neither the name of Adobe Systems Incorporated nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
    THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
    PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
    CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
  /*
  JPEG encoder ported to JavaScript and optimized by Andreas Ritter, www.bytestrom.eu, 11/2009

  */

/**
 * Ported to Java 1.8 by Florent FRADET - 11/2022
 * Removed parts :
 * - RGB -> YCrCb translation (lossless)
 * - YCrCb -> DCT transformation (lossy)
 * - DCT Quantization (lossy)
 */
public class WriterJpeg {

    private static final Log LOG = new Log(WriterJpeg.class);

    private OutputStream out = null;
    private Frame frame = null;
    private Parameters params;

    int bitcode[][] = new int[65535][2];
    int category[] = new int[65535];


    public WriterJpeg(Frame frame, OutputStream out, Parameters p) {
        this.frame = frame;
        this.out = out;
        params = p;

        initCategoryNumber();
        initHuffmanTbl();
    }


    public void write() throws IOException {
        LOG.begin("write");
        bytenew = 0;
        bytepos = 7;

        // Add JPEG headers
        writeWord(0xFFD8); // SOI
        writeApps();
        writeDQT();
        writeSOF0();
        writeDHTs();
        writeSOS();

        int DC[] = new int[frame.comps.size()];
        Arrays.fill(DC, 0);

        bytenew = 0;
        bytepos = 7;

        long timeStart = System.currentTimeMillis();
        ProgressMessage msg = new ProgressMessage(ProgressStepEnum.WRITE, 0);

        int x, y = 0;
        while (y < frame.mcusPerColumn) {
            x = 0;
            while (x < frame.mcusPerLine) {
                int i = 0;
                for (Integer compId : frame.compOrder) {
//                    LOG.debug("i = " + i + " ; compId = " + compId);
                    Component c = frame.comps.get(compId);
                    for (int j = 0; j < c.h * c.v; j++) {
                        int cx = (x * c.h) + (j % c.h);
                        int cy = (y * c.v) + (j / c.h);
//                        LOG.debug("processDU comp " + compId + " x,y = " + cx + "," + cy);

                        DC[i] = processDU(c.blocks[cy][cx], DC[i], (i == 0) ? YDC_HT : UVDC_HT, (i == 0) ? YAC_HT : UVAC_HT);
                    }
                    i++;
                }
                x++;
            }

            long timeCur = System.currentTimeMillis();
            if (timeCur - timeStart > 100) {
                timeStart = timeCur;
                msg.setProgress((double) y / (double) frame.mcusPerColumn);
                params.getProgressCallBack().update(msg);
            }
            y++;
        }

        // Do the bit alignment of the EOI marker
        if (bytepos >= 0) {
            int[] fillbits = new int[2];
            fillbits[1] = bytepos + 1;
            fillbits[0] = (1 << (bytepos + 1)) - 1;
            writeBits(fillbits);
        }

        writeWord(0xFFD9); //EOI

        msg.setProgress((double) y / (double) frame.mcusPerColumn);
        params.getProgressCallBack().update(msg);

        LOG.end("write");
    }


    private void initCategoryNumber() {
        LOG.begin("initCategoryNumber");
        Arrays.fill(category, 0);

        int nrlower = 1;
        int nrupper = 2;
        for (int cat = 1; cat <= 15; cat++) {
            //Positive numbers
            for (int nr = nrlower; nr < nrupper; nr++) {
                category[32767 + nr] = cat;
                bitcode[32767 + nr][1] = cat;
                bitcode[32767 + nr][0] = nr;
            }
            //Negative numbers
            for (int nrneg = -(nrupper - 1); nrneg <= -nrlower; nrneg++) {
                category[32767 + nrneg] = cat;
                bitcode[32767 + nrneg][1] = cat;
                bitcode[32767 + nrneg][0] = nrupper - 1 + nrneg;
            }
            nrlower <<= 1;
            nrupper <<= 1;
        }
        LOG.end("initCategoryNumber");
    }


    private HashMap<Integer, int[]> computeHuffmanTbl(int[] nrcodes, int[] std_table) {
        LOG.begin("computeHuffmanTbl");
        int codevalue = 0;
        int pos_in_table = 0;
        HashMap<Integer, int[]> HT = new HashMap<>();
        for (int k = 0; k < 16; k++) {
            for (int j = 1; j <= nrcodes[k]; j++) {
                int[] tab = new int[2];
                tab[0] = codevalue;
                tab[1] = k + 1;
                HT.put(std_table[pos_in_table], tab);
//                LOG.debug("HT[" + std_table[pos_in_table] + "] = [" + codevalue + "," + k + "]");
                pos_in_table++;
                codevalue++;
            }
            codevalue *= 2;
        }
        LOG.end("computeHuffmanTbl");
        return HT;
    }


    private void initHuffmanTbl() {
        LOG.begin("initHuffmanTbl");
/*
        // Original code
        YDC_HT  = computeHuffmanTbl(std_dc_luminance_nrcodes,   std_dc_luminance_values);
        UVDC_HT = computeHuffmanTbl(std_dc_chrominance_nrcodes, std_dc_chrominance_values);
        YAC_HT  = computeHuffmanTbl(std_ac_luminance_nrcodes,   std_ac_luminance_values);
        UVAC_HT = computeHuffmanTbl(std_ac_chrominance_nrcodes, std_ac_chrominance_values);
*/
        // Replace the tables with default ones (some jpg writers optimizes the huffman tables so that
        // several values are missing, preventing from writing modified DCTs)
        frame.htables.put(0x00, new HuffmanTable(std_dc_luminance_nrcodes, std_dc_luminance_values));
        frame.htables.put(0x01, new HuffmanTable(std_dc_chrominance_nrcodes, std_dc_chrominance_values));
        frame.htables.put(0x10, new HuffmanTable(std_ac_luminance_nrcodes, std_ac_luminance_values));
        frame.htables.put(0x11, new HuffmanTable(std_ac_chrominance_nrcodes, std_ac_chrominance_values));

        YDC_HT = computeHuffmanTbl(frame.htables.get(0x00).nrcodes, frame.htables.get(0x00).values);
        UVDC_HT = computeHuffmanTbl(frame.htables.get(0x01).nrcodes, frame.htables.get(0x01).values);
        YAC_HT = computeHuffmanTbl(frame.htables.get(0x10).nrcodes, frame.htables.get(0x10).values);
        UVAC_HT = computeHuffmanTbl(frame.htables.get(0x11).nrcodes, frame.htables.get(0x11).values);

        LOG.end("initHuffmanTbl");
    }


    private void writeApps() throws IOException {
        LOG.begin("writeApps");
        for (byte[] data : frame.apps) {
            LOG.debug("writing " + Integer.toString((int) data[0] & 0xff, 16) + Integer.toString((int) data[1] & 0xff, 16));
            out.write(data);
        }
        LOG.end("writeApps");
    }


    private void writeSOF0() throws IOException {
        LOG.begin("writeSOF0");
        writeWord(0xFFC0); // marker BaseLine-type encoder
        writeWord(8 + frame.comps.size() * 3);   // length
        writeByte(frame.precision);    // precision
        writeWord(frame.scanLines);
        writeWord(frame.samplesPerLine);
        writeByte(frame.comps.size());    // nrofcomponents

        for (Map.Entry<Integer, Component> i : frame.comps.entrySet()) {
            Component c = i.getValue();
            writeByte(i.getKey());
            writeByte((c.h << 4) | (c.v & 15));
            writeByte(c.qId);
        }
        LOG.end("writeSOF0");
    }

    private void writeDQT() throws IOException {
        LOG.begin("writeDQT");

        for (Map.Entry<Integer, QtzTable> i : frame.qTables.entrySet()) {
            QtzTable qTable = i.getValue();
            int qId = i.getKey();
            int qSpec = ((qTable.type & 0x0F) << 4) | (qId & 0x0F);
            writeWord(0xFFDB); // marker
            writeWord(65 + 2);     // length
            writeByte(qSpec);
            for (int j = 0; j < 64; j++) {
                if (qTable.type == 0)
                    writeByte(qTable.data[j]);
                else
                    writeWord(qTable.data[j]);
            }
        }
        LOG.end("writeDQT");
    }

    private int computeDHTLength(HuffmanTable tab) {
        return 1 + tab.nrcodes.length + tab.values.length;
    }

    private void writeDHT(int type, HuffmanTable tab) throws IOException {
        writeByte(type);
        for (int i = 0; i < tab.nrcodes.length; i++) {
            writeByte(tab.nrcodes[i]);
        }
        for (int i = 0; i < tab.values.length; i++) {
            writeByte(tab.values[i]);
        }
    }

    private void writeDHTs() throws IOException {
        LOG.begin("writeDHTs");

        for (Map.Entry<Integer, HuffmanTable> i : frame.htables.entrySet()) {
            int len = 2 + computeDHTLength(i.getValue());
            writeWord(0xFFC4); // marker
            writeWord(len); // length
            writeDHT(i.getKey(), i.getValue());
        }

        LOG.end("writeDHTs");
    }

    private void writeSOS() throws IOException {
        LOG.begin("writeSOS");
        writeWord(0xFFDA); // marker
        writeWord(2 + 4 + 2 * frame.comps.size()); // length
        writeByte(frame.comps.size()); // nrofcomponents
        for (Map.Entry<Integer, Component> i : frame.comps.entrySet()) {
            writeByte(i.getKey());
            writeByte(i.getValue().huffmanTableSpec);
        }

        writeByte(frame.spectralStart); // Ss
        writeByte(frame.spectralEnd); // Se
        writeByte(0); // Bf
        LOG.end("writeSOS");
    }


    private int processDU(int[] DU_DCT, int DC, HashMap<Integer, int[]> HTDC, HashMap<Integer, int[]> HTAC) throws IOException {
        int[] EOB = HTAC.get(0x00);
        int[] M16zeroes = HTAC.get(0xF0);
        int[] DU = new int[64];
        int pos;
        int I16 = 16;
        int I63 = 63;
        int I64 = 64;
        // ZigZag reorder
        for (int j = 0; j < I64; j++) {
            DU[ZigZag[j]] = DU_DCT[j];
        }
        int Diff = DU[0] - DC;
        DC = DU[0];
        //Encode DC
        if (Diff == 0) {
            writeBits(HTDC.get(0)); // Diff might be 0
        } else {
            pos = 32767 + Diff;
            writeBits(HTDC.get(category[pos]));
            writeBits(bitcode[pos]);
        }
        //Encode ACs
        int end0pos = 63; // was const... which is crazy
        for (; (end0pos > 0) && (DU[end0pos] == 0); end0pos--) {
        }
        ;
        //end0pos = first element in reverse order !=0
        if (end0pos == 0) {
            writeBits(EOB);
            return DC;
        }
        int i = 1;
        int lng;
        while (i <= end0pos) {
            int startpos = i;
            for (; (DU[i] == 0) && (i <= end0pos); ++i) {
            }
            int nrzeroes = i - startpos;
            if (nrzeroes >= I16) {
                lng = nrzeroes >> 4;
                for (int nrmarker = 1; nrmarker <= lng; ++nrmarker)
                    writeBits(M16zeroes);
                nrzeroes = nrzeroes & 0xF;
            }
            pos = 32767 + DU[i];
//            LOG.debug("nrzeroes = " + nrzeroes + " ; pos = " + pos + " ; category[pos] = " + category[pos]);
            writeBits(HTAC.get((nrzeroes << 4) + category[pos]));
            writeBits(bitcode[pos]);
            i++;
        }
        if (end0pos != I63) {
            writeBits(EOB);
        }
        return DC;
    }

    private void writeBits(int[] bs) throws IOException {
        int value = bs[0];
        int posval = bs[1] - 1;
        while (posval >= 0) {
            if ((value & (1 << posval)) != 0) {
                bytenew |= (1 << bytepos);
            }
            posval--;
            bytepos--;
            if (bytepos < 0) {
                if (bytenew == 0xFF) {
                    writeByte(0xFF);
                    writeByte(0);
                } else {
                    writeByte(bytenew);
                }
                bytepos = 7;
                bytenew = 0;
            }
        }
    }

    private void writeByte(int value) throws IOException {
        out.write(value);
    }

    private void writeWord(int value) throws IOException {
        writeByte((value >> 8) & 0xFF);
        writeByte((value) & 0xFF);
    }

    private int bytenew = 0;
    private int bytepos = 7;

    private HashMap<Integer, int[]> YDC_HT;
    private HashMap<Integer, int[]> UVDC_HT;
    private HashMap<Integer, int[]> YAC_HT;
    private HashMap<Integer, int[]> UVAC_HT;


    private int[] ZigZag = new int[]{
            0, 1, 5, 6, 14, 15, 27, 28,
            2, 4, 7, 13, 16, 26, 29, 42,
            3, 8, 12, 17, 25, 30, 41, 43,
            9, 11, 18, 24, 31, 40, 44, 53,
            10, 19, 23, 32, 39, 45, 52, 54,
            20, 22, 33, 38, 46, 51, 55, 60,
            21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63
    };


    // tableSpec = 0x00
    private int[] std_dc_luminance_nrcodes = new int[] {0,1,5,1,1,1,1,1,1,0,0,0,0,0,0,0};
    private int[] std_dc_luminance_values = new int[] {0,1,2,3,4,5,6,7,8,9,10,11};

    // tableSpec = 0x10
    private int[] std_ac_luminance_nrcodes = new int[] {0,2,1,3,3,2,4,3,5,5,4,4,0,0,1,0x7d};
    private int[] std_ac_luminance_values = new int[] {
            0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12,
            0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
            0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08,
            0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
            0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16,
            0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
            0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
            0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
            0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
            0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
            0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
            0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
            0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
            0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
            0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
            0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
            0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
            0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea,
            0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa
    };

    // tableSpec = 0x01
    private int[] std_dc_chrominance_nrcodes = new int[] {0,3,1,1,1,1,1,1,1,1,1,0,0,0,0,0};
    private int[] std_dc_chrominance_values = new int[] {0,1,2,3,4,5,6,7,8,9,10,11};

    // tableSpec = 0x11
    private int[] std_ac_chrominance_nrcodes = new int[] {0,2,1,2,4,4,3,4,7,5,4,4,0,1,2,0x77};
    private int[] std_ac_chrominance_values = new int[]{
            0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
            0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
            0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
            0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
            0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34,
            0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
            0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
            0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
            0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
            0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96,
            0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
            0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4,
            0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
            0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2,
            0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
            0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
            0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa
    };


    // Debug only
    public static void main(String[] args) {
        Log.setLevel(Log.TRACE);
        File f = new File("res/gold.jpg");

        Parameters params = new Parameters();
        InputStream in = null;
        try {
            // Read the original JPG
            BufferedImage img = ImageIO.read(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // Write it as a clean image since JPG can hold multiple progressive scans
            // Progressive scans are not easy to deal with
            ImageIO.write(img, "jpg", bos);

            Log.setLevel(Log.CRITICAL);
            ReaderJpeg reader = new ReaderJpeg(bos.toByteArray(), params);
            reader.decode();

            Log.setLevel(Log.TRACE);
            FileOutputStream fos = new FileOutputStream(new File("res/output.jpg"));
            WriterJpeg writer = new WriterJpeg(reader.getFrame(), fos, params);
            writer.write();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.crit(e.getMessage());
        }

    }

}
