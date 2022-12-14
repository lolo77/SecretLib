package com.secretlib.io.jpeg;

import com.secretlib.model.ProgressMessage;
import com.secretlib.model.ProgressStepEnum;
import com.secretlib.util.HiUtils;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/*
     Copyright 2011 notmasteryet

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
  */

// - The JPEG specification can be found in the ITU CCITT Recommendation T.81
//   (www.w3.org/Graphics/JPEG/itu-t81.pdf)
// - The JFIF specification can be found in the JPEG File Interchange Format
//   (www.w3.org/Graphics/JPEG/jfif3.pdf)
// - The Adobe Application-Specific JPEG markers in the Supporting the DCT Filters
//   in PostScript Level 2, Technical Note #5116
//   (partners.adobe.com/public/developer/en/ps/sdk/5116.DCT_Filter.pdf)

/**
 * Ported to Java 1.8 by Florent FRADET - 11/2022
 *  Removed parts :
 *  - DCT Quantization (lossy)
 *  - DCT -> YCrCb transformation (lossy)
 *  - YCrCb -> RGB translation (lossless)
 */
public class ReaderJpeg {

    private static final Log LOG = new Log(ReaderJpeg.class);

    private byte[] all = null;
    private int offset = 0;
    private Frame frame = null;
    private int resetInterval = 0;

    private HashMap<Integer, HuffNode> huffmanTablesAC = new HashMap<>();
    private HashMap<Integer, HuffNode> huffmanTablesDC = new HashMap<>();
    public HashMap<Integer, HuffmanTable> htables = new HashMap<>();

    private HashMap<Integer, QtzTable> qTables = new HashMap<>();

    public ArrayList<byte[]> apps = new ArrayList<>();

    private int bitsData = 0, bitsCount = 0;
    private int successiveACState = 0, successiveACNextValue;
    private int successive = 0;
    private int eobrun = 0;
    private int spectralStart = 0;
    private int spectralEnd = 63;

    private int[] dctZigZag = new int[]{
            0, 1, 8, 16, 9, 2, 3, 10,
            17, 24, 32, 25, 18, 11, 4, 5,
            12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13, 6, 7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36,
            29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63};

    private Parameters params;

    public ReaderJpeg(byte[] all, Parameters p) {
        this.all = all;
        offset = 0;
        params = p;
    }


    private int readUInt8() {
        int v = (int) all[offset++] & 0xff;
//        LOG.debug("read " + Integer.toString(v, 16) + " ; " + v);
        return v;
    }

    private int readUInt16() {
        int v = (((int) all[offset] << 8) | ((int) all[offset + 1] & 0xff)) & 0xFFFF;
        offset += 2;
//        LOG.debug("read " + Integer.toString(v, 16) + " ; " + v);
        return v;
    }

    private void readAppDataBlock() throws IOException {
        int len = readUInt16();
        byte[] data = Arrays.copyOfRange(all, offset - 4, offset + len - 2);
        offset += len - 2;
        apps.add(data);
    }

    private boolean isEOF() {
        return offset >= all.length;
    }

    private void prepareComponents() throws Exception {
        if (frame == null) {
            throw new Exception("No frame");
        }
        int maxH = 0, maxV = 0;
        for (Component component : frame.comps.values()) {
            if (maxH < component.h) maxH = component.h;
            if (maxV < component.v) maxV = component.v;
        }
//        LOG.debug("max H,V = " + maxH + "," + maxV);

        int mcusPerLine = (int) Math.ceil((double) frame.samplesPerLine / 8 / maxH);
        int mcusPerColumn = (int) Math.ceil((double) frame.scanLines / 8 / maxV);
//        LOG.debug("mcusPerLine = " + mcusPerLine);
//        LOG.debug("mcusPerColumn = " + mcusPerColumn);
        for (Component component : frame.comps.values()) {

//            LOG.debug("Comp H,V = " + component.h + "," + component.v);

            int blocksPerLine = (int) Math.ceil(Math.ceil((double) frame.samplesPerLine / 8) * (double) component.h / (double) maxH);
            int blocksPerColumn = (int) Math.ceil(Math.ceil((double) frame.scanLines / 8) * (double) component.v / (double) maxV);
            int blocksPerLineForMcu = mcusPerLine * component.h;
            int blocksPerColumnForMcu = mcusPerColumn * component.v;

//            LOG.debug("blocksPerLine = " + blocksPerLine);
//            LOG.debug("blocksPerColumn = " + blocksPerColumn);

//            LOG.debug("blocksPerLineForMcu = " + blocksPerLineForMcu);
//            LOG.debug("blocksPerColumnForMcu = " + blocksPerColumnForMcu);

            int[][][] blocks = new int[blocksPerColumnForMcu][blocksPerLineForMcu][64];

            component.blocksPerLine = blocksPerLine;
            component.blocksPerColumn = blocksPerColumn;
            component.blocks = blocks;

        }
        frame.maxH = maxH;
        frame.maxV = maxV;
        frame.mcusPerLine = mcusPerLine;
        frame.mcusPerColumn = mcusPerColumn;
    }


    private HuffNode buildHuffmanTable(int[] codeLengths, int[] values) {
//		LOG.debug("buildHuffmanTable : codeLengths = " + Arrays.toString(codeLengths));
//        LOG.debug("buildHuffmanTable : values = " + Arrays.toString(values));

        ArrayList<HuffNode> code = new ArrayList<>();
        int k = 0;
        int length = codeLengths.length;
        while (length > 0 && (codeLengths[length - 1] == 0))
            length--;

//        LOG.debug("length = " + length);

        HuffNode p = new HuffNode();
        code.add(p);
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < codeLengths[i]; j++) {
                p = code.get(code.size() - 1);
                code.remove(code.size() - 1);

                p.data[p.index] = Byte.valueOf((byte) values[k]);
                while (p.index > 0) {
                    p = code.get(code.size() - 1);
                    code.remove(code.size() - 1);
                }
                p.index++;
                code.add(p);
                while (code.size() <= i) {
                    HuffNode q = new HuffNode();
                    code.add(q);
                    p.children[p.index] = q;
                    p = q;
                }
                k++;
            }
            if (i + 1 < length) {
                HuffNode q = new HuffNode();
                code.add(q);
                p.children[p.index] = q;
                p = q;
            }
        }

//        HuffNode.dumpTree(code.get(0), 0, "");
        return code.get(0);
    }

    private int readBit() {
        if (bitsCount > 0) {
            bitsCount--;
            return (bitsData >> bitsCount) & 1;
        }
        if (isEOF()) {
            return -1;
        }
        bitsData = readUInt8();
        if (bitsData == 0xFF) {
            int nextByte = readUInt8();
            if (nextByte > 0) {
                LOG.crit("unexpected marker: " + Integer.toString(((bitsData << 8) | nextByte), 16));
                return -1;
            }
            // unstuff 0
        }
        bitsCount = 7;
        return bitsData >>> 7;
    }

    private int decodeHuffman(HuffNode tree) throws Exception {
        HuffNode node = tree;
        int bit = readBit();
        while (bit >= 0) {
            if (node.data[bit] != null)
                return (int) node.data[bit] & 0xff;
            if (node.children[bit] == null)
                throw new Exception("Error in huffman decoding");
            node = node.children[bit];
            bit = readBit();
        }
        return -1;
    }

    private int receive(int length) {
        int n = 0;
        while (length > 0) {
            int bit = readBit();
            if (bit < 0)
                return -1;
            n = (n << 1) | bit;
            length--;
        }
        return n;
    }

    private int receiveAndExtend(int length) {
        int n = receive(length);
        if (n < 0)
            return -1;

        if (n >= 1 << (length - 1))
            return n;

        return n + (-1 << length) + 1;
    }


    private interface IDecoder {
        public void decode(Component component, int[] block) throws Exception;
    }

    private class DecoderBaseLine implements IDecoder {
        public void decode(Component component, int[] zz) throws Exception {
            int t = decodeHuffman(component.huffmanTableDC);
            int diff = (t == 0) ? 0 : receiveAndExtend(t);
            zz[0] = (component.pred += diff);
            int k = 1;
            while (k < 64) {
                int rs = decodeHuffman(component.huffmanTableAC);
                int s = rs & 15, r = rs >> 4;
                if (s == 0) {
                    if (r < 15)
                        break;
                    k += 16;
                    continue;
                }
                k += r;
                int z = dctZigZag[k];
                zz[z] = receiveAndExtend(s);
                k++;
            }
        }
    }

    private class DecoderDCFirst implements IDecoder {
        public void decode(Component component, int[] block) throws Exception {
            int t = decodeHuffman(component.huffmanTableDC);
            int diff = (t == 0) ? 0 : (receiveAndExtend(t) << successive);
            block[0] = (component.pred += diff);
        }
    }

    private class DecoderACFirst implements IDecoder {
        public void decode(Component component, int[] block) throws Exception {
            if (eobrun > 0) {
                eobrun--;
                return;
            }
            int k = spectralStart, e = spectralEnd;
            while (k <= e) {
                int rs = decodeHuffman(component.huffmanTableAC);
                int s = rs & 15, r = rs >> 4;
                if (s == 0) {
                    if (r < 15) {
                        eobrun = receive(r) + (1 << r) - 1;
                        break;
                    }
                    k += 16;
                    continue;
                }
                k += r;
                int z = dctZigZag[k];
                block[z] = receiveAndExtend(s) * (1 << successive);
                k++;
            }
        }
    }

    private class DecoderDCSuccessive implements IDecoder {
        public void decode(Component component, int[] block) {
            block[0] |= readBit() << successive;
        }
    }

    private class DecoderACSuccessive implements IDecoder {
        public void decode(Component component, int[] block) throws Exception {
            int k = spectralStart, e = spectralEnd, r = 0;
            while (k <= e) {
                int z = dctZigZag[k];
                int direction = block[z] < 0 ? -1 : 1;
                switch (successiveACState) {
                    case 0: // initial state
                        int rs = decodeHuffman(component.huffmanTableAC);
                        int s = rs & 15;
                        r = rs >> 4;
                        if (s == 0) {
                            if (r < 15) {
                                eobrun = receive(r) + (1 << r);
                                successiveACState = 4;
                            } else {
                                r = 16;
                                successiveACState = 1;
                            }
                        } else {
                            if (s != 1) {
                                LOG.crit("invalid ACn encoding");
                                return;
                            }
                            successiveACNextValue = receiveAndExtend(s);
                            successiveACState = (r != 0) ? 2 : 3;
                        }
                        continue;
                    case 1: // skipping r zero items
                    case 2:
                        if (block[z] != 0)
                            block[z] += (readBit() << successive) * direction;
                        else {
                            r--;
                            if (r == 0)
                                successiveACState = successiveACState == 2 ? 3 : 0;
                        }
                        break;
                    case 3: // set value for a zero item
                        if (block[z] != 0)
                            block[z] += (readBit() << successive) * direction;
                        else {
                            block[z] = successiveACNextValue << successive;
                            successiveACState = 0;
                        }
                        break;
                    case 4: // eob
                        if (block[z] != 0)
                            block[z] += (readBit() << successive) * direction;
                        break;
                }
                k++;
            }
            if (successiveACState == 4) {
                eobrun--;
                if (eobrun == 0)
                    successiveACState = 0;
            }
        }
    }


    private void decodeMcu(Component component, IDecoder decoder, int mcu, int row, int col) throws Exception {
        int mcuRow = (frame.mcusPerLine == 0) ? 0 : (mcu / frame.mcusPerLine);
        int mcuCol = (frame.mcusPerLine == 0) ? 0 : (mcu % frame.mcusPerLine);
        int blockRow = mcuRow * component.v + row;
        int blockCol = mcuCol * component.h + col;

//		LOG.debug("decode mcu block x,y : " + blockCol + "," + blockRow);

        decoder.decode(component, component.blocks[blockRow][blockCol]);

//        LOG.debug("Decoded MCU DCT block : " + Arrays.toString(component.blocks[blockRow][blockCol]));
    }


    private void decodeBlock(Component component, IDecoder decoder, int mcu) throws Exception {
        int blockRow = (component.blocksPerLine == 0) ? 0 : (mcu / component.blocksPerLine);
        int blockCol = (component.blocksPerLine == 0) ? 0 : (mcu % component.blocksPerLine);

//        LOG.debug("decode block mcu,x,y : " + mcu + "," + blockCol + "," + blockRow);

        decoder.decode(component, component.blocks[blockRow][blockCol]);

//        LOG.debug("Decoded DCT block : " + Arrays.toString(component.blocks[blockRow][blockCol]));
    }


    private void decodeScan(ArrayList<Component> comps, int resetInterval,
                            int successivePrev, int successive) throws Exception {

        boolean progressive = frame.progressive;
        int maxH = frame.maxH, maxV = frame.maxV;
        this.successive = successive;
        eobrun = 0;

        bitsData = 0;
        bitsCount = 0;

        int componentsLength = comps.size();
        Component component;
        int i, j, k, n;

        IDecoder decodeFn;
        if (progressive) {
            if (spectralStart == 0)
                decodeFn = (successivePrev == 0) ? new DecoderDCFirst() : new DecoderDCSuccessive();
            else
                decodeFn = (successivePrev == 0) ? new DecoderACFirst() : new DecoderACSuccessive();
        } else {
            decodeFn = new DecoderBaseLine();
        }

        int mcu = 0, marker;
        int mcuExpected;
        if (componentsLength == 1) {
            mcuExpected = comps.get(0).blocksPerLine * comps.get(0).blocksPerColumn;
//            LOG.debug("blocksPerLine = " + comps.get(0).blocksPerLine + " ; blocksPerColumn = " + comps.get(0).blocksPerColumn);
        } else {
            mcuExpected = frame.mcusPerLine * frame.mcusPerColumn;
        }
        if (resetInterval == 0)
            resetInterval = mcuExpected;

        // Secure
        if (resetInterval > mcuExpected) {
            resetInterval = mcuExpected;
        }

//        LOG.debug("DecodeScan : resetInterval = " + resetInterval + " ; mcuExpected = " + mcuExpected + " ; componentsLength = " + componentsLength);

        long timeStart = System.currentTimeMillis();
        ProgressMessage msg = new ProgressMessage(ProgressStepEnum.READ, 0);

        int h, v;
        while (mcu < mcuExpected) {
            // reset interval stuff
            for (i = 0; i < componentsLength; i++)
                comps.get(i).pred = 0;
            eobrun = 0;

            if (componentsLength == 1) {
                component = comps.get(0);
                for (n = 0; n < resetInterval; n++) {
                    decodeBlock(component, decodeFn, mcu);
                    mcu++;
                }
            } else {
                for (n = 0; n < resetInterval; n++) {
                    for (i = 0; i < componentsLength; i++) {
                        component = comps.get(i);
                        h = component.h;
                        v = component.v;

                        for (j = 0; j < v; j++) {
                            for (k = 0; k < h; k++) {
                                decodeMcu(component, decodeFn, mcu, j, k);
                            }
                        }
                    }
                    mcu++;

                    long timeCur = System.currentTimeMillis();
                    if (timeCur - timeStart > 100) {
                        timeStart = timeCur;
                        msg.setProgress((double) mcu / (double) mcuExpected);
                        params.getProgressCallBack().update(msg);
                    }

                    // If we've reached our expected MCU's, stop decoding
                    if (mcu == mcuExpected)
                        break;
                }
            }

            // find marker
            bitsCount = 0;
            marker = readUInt16();
            if (marker < 0xFF00) {
                throw new Exception("marker was not found");
            }

            if (!(marker >= 0xFFD0 && marker <= 0xFFD7)) { // RSTx
                offset -= 2;
                break;
            }
        }

        msg.setProgress((double) mcu / (double) mcuExpected);
        params.getProgressCallBack().update(msg);
    }


    public boolean decode() throws Exception {
        LOG.begin("decode");
        int nbSOS = 0;
        int chunkMarker = readUInt16();
        if (chunkMarker != 0xFFD8) {
            throw new Exception("Not a JPG header : expected FFD8, read " + Integer.toString(chunkMarker, 16));
        }
        while (chunkMarker != 0xFFD9) {
            chunkMarker = readUInt16();
            LOG.debug("Chunk " + Integer.toString(chunkMarker, 16));

            switch (chunkMarker) {
                case 0xFF00:
                    break; // Encoded FF byte
                case 0xFFE0: // APP0 (Application Specific)
                case 0xFFE1: // APP1
                case 0xFFE2: // APP2
                case 0xFFE3: // APP3
                case 0xFFE4: // APP4
                case 0xFFE5: // APP5
                case 0xFFE6: // APP6
                case 0xFFE7: // APP7
                case 0xFFE8: // APP8
                case 0xFFE9: // APP9
                case 0xFFEA: // APP10
                case 0xFFEB: // APP11
                case 0xFFEC: // APP12
                case 0xFFED: // APP13
                case 0xFFEE: // APP14
                case 0xFFEF: // APP15
                case 0xFFFE: // COM (Comment)
                    readAppDataBlock();
                    break;
                case 0xFFDB: // DQT (Define Quantization Tables)
                    LOG.debug("DQT - Define Quantization Tables");
                    int quantizationTablesLength = readUInt16();
                    int quantizationTablesEnd = quantizationTablesLength + offset - 2;
                    while (offset < quantizationTablesEnd) {
                        int quantizationTableSpec = readUInt8();
                        int quantizationTableId = quantizationTableSpec & 15;
                        int[] tableData = new int[64];
                        if ((quantizationTableSpec >> 4) == 0) { // 8 bit values
                            for (int j = 0; j < 64; j++) {
                                //int z = dctZigZag[j];
                                tableData[j] = readUInt8();
                            }
                        } else if ((quantizationTableSpec >> 4) == 1) { //16 bit
                            for (int j = 0; j < 64; j++) {
                                //int z = dctZigZag[j];
                                tableData[j] = readUInt16();
                            }
                        } else
                            throw new Exception("DQT: invalid table spec");
                        QtzTable tab = new QtzTable();
                        tab.type = quantizationTableSpec >> 4;
                        tab.data = tableData;
                        qTables.put(quantizationTableId, tab);
                        LOG.debug("Quantization Table #" + quantizationTableId);
                    }
                    if (frame != null) {
                        frame.qTables = qTables;
                    }
                    break;

                case 0xFFC0: // SOF0 (Start of Frame, Baseline DCT)
                case 0xFFC1: // SOF1 (Start of Frame, Extended DCT)
                case 0xFFC2: // SOF2 (Start of Frame, Progressive DCT)
                    LOG.debug("SOF" + (chunkMarker & 3) + " (Start Of Frame)");

                    readUInt16(); // skip length
                    if (frame != null) {
                        throw new Exception("Multi frames not supported");
                    }
                    frame = new Frame();
                    frame.apps = apps;
                    frame.qTables = qTables;
                    frame.htables = htables;
                    frame.extended = (chunkMarker == 0xFFC1);
                    frame.progressive = (chunkMarker == 0xFFC2);
                    frame.precision = readUInt8();
                    frame.scanLines = readUInt16();
                    frame.samplesPerLine = readUInt16();
                    frame.spectralStart = spectralStart;
                    frame.spectralEnd = spectralEnd;
                    LOG.debug("extended = " + frame.extended);
                    LOG.debug("progressive = " + frame.progressive);
                    LOG.debug("precision = " + frame.precision);
                    LOG.debug("scanLines = " + frame.scanLines);
                    LOG.debug("samplesPerLine = " + frame.samplesPerLine);

                    int compCount = readUInt8();
                    LOG.debug("compCount = " + compCount);
                    for (int i = 0; i < compCount; i++) {
                        Component c = new Component();
                        int id = readUInt8();
                        int b = readUInt8();
                        c.h = b >> 4;
                        c.v = b & 0x0F;
                        c.qId = readUInt8();
                        frame.comps.put(id, c);
                        frame.compOrder.add(id);
                    }
                    prepareComponents();
                    break;

                case 0xFFC4: // DHT (Define Huffman Tables)
                    int huffmanLength = readUInt16();
                    LOG.debug("DHT - Define Huffman Table");
                    for (int i = 2; i < huffmanLength; ) {
                        int huffmanTableSpec = readUInt8();
                        int[] codeLengths = new int[16];
                        int huffTblId = huffmanTableSpec & 15;

//                        LOG.debug("Huffman table spec 0x" + Integer.toString(huffmanTableSpec, 16) + " : # " + huffTblId + " / " + (((huffmanTableSpec >> 4) == 0) ? "DC" : "AC"));

                        int codeLengthSum = 0;
                        for (int j = 0; j < 16; j++)
                            codeLengthSum += (codeLengths[j] = readUInt8());

                        int[] huffmanValues = new int[codeLengthSum];
                        for (int j = 0; j < codeLengthSum; j++)
                            huffmanValues[j] = readUInt8();

                        HuffmanTable t = new HuffmanTable(codeLengths, huffmanValues);
                        htables.put(huffmanTableSpec, t);

                        i += 17 + codeLengthSum;

                        HashMap<Integer, HuffNode> table = huffmanTablesAC;
                        if ((huffmanTableSpec >> 4) == 0) {
                            table = huffmanTablesDC;
                        }
//                        LOG.debug("huffmanTableSpec = " + huffmanTableSpec);
//                        LOG.debug("codeLengths = " + Arrays.toString(codeLengths));
//                        LOG.debug("huffmanValues = " + Arrays.toString(huffmanValues));
                        table.put(huffTblId, buildHuffmanTable(codeLengths, huffmanValues));
                    }
                    break;

                case 0xFFDD: // DRI (Define Restart Interval)
                    readUInt16(); // skip data length
                    resetInterval = readUInt16();
                    LOG.debug("DRI (Define Restart Interval) = " + resetInterval);
                    break;

                case 0xFFFF: // Fill bytes
                    if (all[offset] != 0xFF) { // Avoid skipping a valid marker.
                        offset--;
                    }
                    break;

                case 0xFFDA: // SOS (Start of Scan)
                    LOG.debug("SOS (Start of Scan)");
                    nbSOS++;
                    if (nbSOS > 1) {
                        throw new Exception("Multiple SOS not supported");
                    }
                    readUInt16(); // skip length (always mistaken 12)

                    int selectorsCount = readUInt8();
                    LOG.debug("selectorsCount = " + selectorsCount);
                    ArrayList<Component> components = new ArrayList<>();
                    Component component;
                    for (int i = 0; i < selectorsCount; i++) {
                        int compId = readUInt8();
//                        LOG.debug("compId = " + compId);
                        component = frame.comps.get(compId);
                        int tableSpec = readUInt8();
//                        LOG.debug("tableSpec = 0x" + Integer.toString(tableSpec,16));
                        component.huffmanTableSpec = tableSpec;
                        component.huffmanTableDC = huffmanTablesDC.get(tableSpec >> 4);
                        component.huffmanTableAC = huffmanTablesAC.get(tableSpec & 15);
                        components.add(component);
                    }
                    spectralStart = readUInt8();
                    spectralEnd = readUInt8();
                    int successiveApproximation = readUInt8();
                    if (frame != null) {
                        frame.spectralStart = spectralStart;
                        frame.spectralEnd = spectralEnd;
                    }
//                    LOG.debug("spectralStart, spectralEnd, successiveApproximation = " + spectralStart + "," + spectralEnd + "," + successiveApproximation);
                    decodeScan(components, resetInterval, successiveApproximation >> 4, successiveApproximation & 15);
                    break;

                case 0xFFD9:
                    LOG.debug("End of JPG");
                    break;

                default:
                    if (all[offset - 3] == 0xFF &&
                            all[offset - 2] >= 0xC0 &&
                            all[offset - 2] <= 0xFE) {
                        // could be incorrect encoding -- last 0xFF byte of the previous
                        // block was eaten by the encoder
                        offset -= 3;
                        break;
                    }
                    throw new Exception("unknown JPEG marker " + Integer.toString(chunkMarker, 16));
            }
        }
        all = null;
        LOG.end("decode");
        return true;
    }

    public Frame getFrame() {
        return frame;
    }


    // Debug only
    public static void main(String[] args) {
        Log.setLevel(Log.TRACE);
        File f = new File("res/tmp.jpg");
        InputStream in = null;
        try {
            in = new FileInputStream((f));
            ReaderJpeg reader = new ReaderJpeg(HiUtils.readAllBytes(in), new Parameters());
            reader.decode();

            int[] block = reader.getFrame().comps.get(1).blocks[0][0];
            for (int y = 0; y < 8; y++) {
                String sLine = "";
                for (int x = 0; x < 8; x++) {
                    sLine += " " + block[x + y * 8];
                }
                LOG.debug(sLine);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOG.crit(e.getMessage());
        }

    }
}
