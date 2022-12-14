package com.secretlib.model;

import com.secretlib.exception.TruncatedBagException;
import com.secretlib.util.Log;
import com.secretlib.util.Parameters;
import com.secretlib.util.HiUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * @author Florent FRADET
 * <p>
 * Data struture :
 * <p>
 * DataFrag Type : 1 byte
 * Raw DataFrag Length : 3 bytes
 * HEADER : 4 bytes
 * Real length delta : 1 byte (real length = Raw DataFrag Length + Real length delta)
 * data : Raw DataFrag Length - 5
 * <p>
 * If HEADER is clearly readable : data is not encrypted
 * If not, try to uncipher the data. If HEADER is still not readable : data remains encrypted
 */
public class ChunkData extends AbstractChunk {

    private static final Log LOG = new Log(ChunkData.class);

    private static final byte[] HEADER = new byte[]{(byte) 0xFF, (byte) 0x0F, (byte) 0xEF, (byte) 0x78};

    // A name given to this data block
    private String name = null;

    // The decrypted data
    private byte[] data = new byte[0];

    // If true, the raw data is stored in rawData and data is empty
    // If false, the raw data is stored in rawData and data holds the decrypted data
    private boolean encrypted = false;

    public ChunkData() {
        super();
        type = EChunk.DATA;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * Encrypts data[] into rawData[]
     *
     * @param p encryption params
     */
    public void encryptData(Parameters p) throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            NoSuchPaddingException,
            IllegalBlockSizeException,
            BadPaddingException {
        if (!encrypted) {
            if (data == null) {
                rawData = new byte[0];
                return;
            }
            byte[] bufName = (name == null) ? new byte[0] : name.getBytes(StandardCharsets.UTF_8);
            int len = HEADER.length + 4 + bufName.length;

            byte[] all = new byte[len + data.length];
            int ofs = 0;
            System.arraycopy(HEADER, 0, all, ofs, HEADER.length);
            ofs += HEADER.length;
            all[ofs++] = (byte)bufName.length;
            all[ofs++] = (byte) ((int) data.length & 0xFF);
            all[ofs++] = (byte) ((int) (data.length >> 8) & 0xFF);
            all[ofs++] = (byte) ((int) (data.length >> 16) & 0xFF);
            System.arraycopy(bufName, 0, all, ofs, bufName.length);
            ofs += bufName.length;
            System.arraycopy(data, 0, all, ofs, data.length);

            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(p.getKd().toCharArray(), HiUtils.genHash(p.getKm().getBytes(StandardCharsets.UTF_8), p.getHashAlgo()), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            rawData = cipher.doFinal(all);

        }
    }


    protected boolean tryParseData(byte[] theData) throws TruncatedBagException {
        if (theData == null) {
            name = null;
            data = null;
            return true; // Nothing to parse = ok
        }
        if (HiUtils.arraysEquals(theData, 0, HEADER, 0, HEADER.length)) {
            // Not encrypted
            int offset = HEADER.length;
            int nameLen = readInt8(theData, offset++);

            int dataLen = readInt24(theData, offset);
            offset += 3;

            if ((offset + nameLen + dataLen) > theData.length)
                throw new TruncatedBagException();

            byte buf[] = Arrays.copyOfRange(theData, offset, offset + nameLen);
            offset += nameLen;
            name = new String(buf);
            LOG.debug("Name : " + name);
            data = Arrays.copyOfRange(theData, offset, offset + dataLen);
            return true;
        }
        return false;
    }


    public void decryptData(Parameters p) throws TruncatedBagException, GeneralSecurityException {
        encrypted = !tryParseData(rawData);
        if (!encrypted)
            return;

        byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        IvParameterSpec ivspec = new IvParameterSpec(iv);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(p.getKd().toCharArray(), HiUtils.genHash(p.getKm().getBytes(StandardCharsets.UTF_8), p.getHashAlgo()), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
        try {
            byte[] deciphered = cipher.doFinal(rawData);
            encrypted = !tryParseData(deciphered);
        } catch (Exception e) {
            LOG.debug("Decipher exception : " + e.getMessage());
        }
    }


    public void write(OutputStream s) throws IOException {
        s.write(isEncrypted() ? rawData : data);
    }

    @Override
    public String toString() {
        return "ChunkData{" +
                "name=" + ((name != null) ? name : "") +
                ", data=" + HiUtils.toStringHex(data, 16) +
                ", rawData=" + HiUtils.toStringHex(rawData, 16) +
                "}";
    }
}
