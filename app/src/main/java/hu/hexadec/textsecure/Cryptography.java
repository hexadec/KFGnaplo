package hu.hexadec.textsecure;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import android.util.Base64;

/**
 * This is a basic tool to encrypt {@link String} without having to use too complicated API-s <br/>
 * The strength of the algorithms is not tested! The strongest one is <i>Threedog</i> (which uses {ThreedogDouble} and {ThreedogTreble})
 * The methods aren't static now, but you can use <i><b>new EncryptUtils().</b></i> if you don't want to use the non-static functions. (Changing the block size and the encoding)
 * <br><br><b>Threedog version:</b> <i>2</i>
 *
 * @author Cseh András
 * @version 1.1-ULTRALIGHT.3
 */
public class Cryptography {
    private final String ENCODING = "UTF-8";

    public static final String version = "1.1-ULTRALIGHT.3";
    private int BLOCK_SIZE = 128;
    private int BSBYTE = BLOCK_SIZE / 8;

    public String base64encode(String text) {
        try {
            if (text == null) throw new Exception();
            return Base64.encodeToString(text.getBytes(ENCODING), Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    public String base64decode(String text) {

        try {
            if (text == null) throw new Exception();
            return new String(Base64.decode(text, Base64.DEFAULT), ENCODING);
        } catch (Exception e) {
            return null;
        }
    }

    private String threedogBlockFill(String text, int BLOCKSIZE_BYTE) throws Exception {
        if (text == null) return null;
        if (text.equals("")) return ".";
        String s = base64encode(text);
        while (s.getBytes(ENCODING).length < BLOCKSIZE_BYTE + 16) {
            s += s;
            s = base64encode(s);
        }
        return s;
    }

    public String decryptSTEP11(String message, String key) {
        try {

            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();
            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];
            for (int i = 0; i < ml; i++) {
                mesg[i] = (char) (mesg[i] ^ keys[(i) % kl]);
                mesg[i] = (char) (mesg[i] ^ keys[(((kl + ml) - i)) % kl]);
                newmsg[i] = (char) (((mesg[i] - (keys[(i + 99) % kl] - keys[((i + 121) % kl)] ^ keys[(i + 154) % kl]) - (~(keys[(i + 11) % kl] ^ keys[(i + 44) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 22) % kl]) ^ keys[(i + 33) % kl]) - (keys[((kl * (keys[(i + 77) % kl]) ^ (keys[((i + 55) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 63) % kl] - keys[((i + 77) % kl)] ^ keys[(i + 98) % kl]) - (~(keys[(i + 7) % kl] ^ keys[(i + 28) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 14) % kl]) ^ keys[(i + 21) % kl]) - (keys[((kl * (keys[(i + 49) % kl]) ^ (keys[((i + 35) % kl)]))) % kl]));


                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 27) % kl] - keys[((i + 33) % kl)] ^ keys[(i + 42) % kl]) - (~(keys[(i + 3) % kl] ^ keys[(i + 12) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 6) % kl]) ^ keys[(i + 9) % kl]) - (keys[((kl * (keys[(i + 21) % kl]) ^ (keys[((i + 15) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 18) % kl] - keys[((i + 22) % kl)] ^ keys[(i + 28) % kl]) - (~(keys[(i + 2) % kl] ^ keys[(i + 8) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 4) % kl]) ^ keys[(i + 6) % kl]) - (keys[((kl * (keys[(i + 14) % kl]) ^ (keys[((i + 10) % kl)]))) % kl]));
                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 9) % kl] - keys[((i + 11) % kl)] ^ keys[(i + 14) % kl]) - (~(keys[(i + 1) % kl] ^ keys[(i + 4) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 2) % kl]) ^ keys[(i + 3) % kl]) - (keys[((kl * (keys[(i + 7) % kl]) ^ (keys[((i + 5) % kl)]))) % kl]));

            }
            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }

    public String encryptSTEP11(String message, String key) {
        try {

            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();
            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];
            for (int i = 0; i < ml; i++) {

                newmsg[i] = (char) (((mesg[i] + keys[((kl * (keys[(i + 7) % kl]) ^ (keys[((i + 5) % kl)]))) % kl]) ^ keys[(i + 3) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 2) % kl])) + (~(keys[(i + 4) % kl]) ^ keys[(i + 1) % kl]) + (keys[(i + 9) % kl] - keys[((i + 11) % kl)] ^ keys[(i + 14) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 14) % kl]) ^ (keys[((i + 10) % kl)]))) % kl]) ^ keys[(i + 6) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 4) % kl])) + (~(keys[(i + 8) % kl]) ^ keys[(i + 2) % kl]) + (keys[(i + 18) % kl] - keys[((i + 22) % kl)] ^ keys[(i + 28) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 21) % kl]) ^ (keys[((i + 15) % kl)]))) % kl]) ^ keys[(i + 9) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 6) % kl])) + (~(keys[(i + 12) % kl]) ^ keys[(i + 3) % kl]) + (keys[(i + 27) % kl] - keys[((i + 33) % kl)] ^ keys[(i + 42) % kl]));

                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 49) % kl]) ^ (keys[((i + 35) % kl)]))) % kl]) ^ keys[(i + 21) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 14) % kl])) + (~(keys[(i + 28) % kl]) ^ keys[(i + 7) % kl]) + (keys[(i + 63) % kl] - keys[((i + 77) % kl)] ^ keys[(i + 98) % kl]));

                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 77) % kl]) ^ (keys[((i + 55) % kl)]))) % kl]) ^ keys[(i + 33) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 22) % kl])) + (~(keys[(i + 44) % kl]) ^ keys[(i + 11) % kl]) + (keys[(i + 99) % kl] - keys[((i + 121) % kl)] ^ keys[(i + 154) % kl]));

                newmsg[i] = (char) (newmsg[i] ^ keys[(((kl + ml) - i)) % kl]);
                newmsg[i] = (char) (newmsg[i] ^ keys[(i) % kl]);
            }
            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }

    public String decryptSTEP13X(String message, String key) {
        try {

            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();
            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];
            for (int i = 0; i < ml; i++) {
                mesg[i] = (char) (mesg[i] ^ keys[(i) % kl]);
                mesg[i] = (char) (mesg[i] ^ keys[(((kl + ml) - i)) % kl]);
                newmsg[i] = (char) (((mesg[i] - (keys[(i + 111) % kl] - keys[((i + 143) % kl)] ^ keys[(i + 182) % kl]) - (~(keys[(i + 13) % kl] ^ keys[(i + 52) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 26) % kl]) ^ keys[(i + 39) % kl]) - (keys[((kl * (keys[(i + 91) % kl]) ^ (keys[((i + 65) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 99) % kl] - keys[((i + 121) % kl)] ^ keys[(i + 154) % kl]) - (~(keys[(i + 11) % kl] ^ keys[(i + 44) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 22) % kl]) ^ keys[(i + 33) % kl]) - (keys[((kl * (keys[(i + 77) % kl]) ^ (keys[((i + 55) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 63) % kl] - keys[((i + 77) % kl)] ^ keys[(i + 98) % kl]) - (~(keys[(i + 7) % kl] ^ keys[(i + 28) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 14) % kl]) ^ keys[(i + 21) % kl]) - (keys[((kl * (keys[(i + 49) % kl]) ^ (keys[((i + 35) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 45) % kl] - keys[((i + 55) % kl)] ^ keys[(i + 70) % kl]) - (~(keys[(i + 5) % kl] ^ keys[(i + 20) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 10) % kl]) ^ keys[(i + 15) % kl]) - (keys[((kl * (keys[(i + 35) % kl]) ^ (keys[((i + 25) % kl)]))) % kl]));
                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 27) % kl] - keys[((i + 33) % kl)] ^ keys[(i + 42) % kl]) - (~(keys[(i + 3) % kl] ^ keys[(i + 12) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 6) % kl]) ^ keys[(i + 9) % kl]) - (keys[((kl * (keys[(i + 21) % kl]) ^ (keys[((i + 15) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 18) % kl] - keys[((i + 22) % kl)] ^ keys[(i + 28) % kl]) - (~(keys[(i + 2) % kl] ^ keys[(i + 8) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 4) % kl]) ^ keys[(i + 6) % kl]) - (keys[((kl * (keys[(i + 14) % kl]) ^ (keys[((i + 10) % kl)]))) % kl]));
                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 9) % kl] - keys[((i + 11) % kl)] ^ keys[(i + 14) % kl]) - (~(keys[(i + 1) % kl] ^ keys[(i + 4) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 2) % kl]) ^ keys[(i + 3) % kl]) - (keys[((kl * (keys[(i + 7) % kl]) ^ (keys[((i + 5) % kl)]))) % kl]));

            }
            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }

    public String encryptSTEP13X(String message, String key) {
        try {

            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();
            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];
            for (int i = 0; i < ml; i++) {

                newmsg[i] = (char) (((mesg[i] + keys[((kl * (keys[(i + 7) % kl]) ^ (keys[((i + 5) % kl)]))) % kl]) ^ keys[(i + 3) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 2) % kl])) + (~(keys[(i + 4) % kl]) ^ keys[(i + 1) % kl]) + (keys[(i + 9) % kl] - keys[((i + 11) % kl)] ^ keys[(i + 14) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 14) % kl]) ^ (keys[((i + 10) % kl)]))) % kl]) ^ keys[(i + 6) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 4) % kl])) + (~(keys[(i + 8) % kl]) ^ keys[(i + 2) % kl]) + (keys[(i + 18) % kl] - keys[((i + 22) % kl)] ^ keys[(i + 28) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 21) % kl]) ^ (keys[((i + 15) % kl)]))) % kl]) ^ keys[(i + 9) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 6) % kl])) + (~(keys[(i + 12) % kl]) ^ keys[(i + 3) % kl]) + (keys[(i + 27) % kl] - keys[((i + 33) % kl)] ^ keys[(i + 42) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 35) % kl]) ^ (keys[((i + 25) % kl)]))) % kl]) ^ keys[(i + 15) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 10) % kl])) + (~(keys[(i + 20) % kl]) ^ keys[(i + 5) % kl]) + (keys[(i + 45) % kl] - keys[((i + 55) % kl)] ^ keys[(i + 70) % kl]));

                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 49) % kl]) ^ (keys[((i + 35) % kl)]))) % kl]) ^ keys[(i + 21) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 14) % kl])) + (~(keys[(i + 28) % kl]) ^ keys[(i + 7) % kl]) + (keys[(i + 63) % kl] - keys[((i + 77) % kl)] ^ keys[(i + 98) % kl]));

                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 77) % kl]) ^ (keys[((i + 55) % kl)]))) % kl]) ^ keys[(i + 33) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 22) % kl])) + (~(keys[(i + 44) % kl]) ^ keys[(i + 11) % kl]) + (keys[(i + 99) % kl] - keys[((i + 121) % kl)] ^ keys[(i + 154) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 91) % kl]) ^ (keys[((i + 65) % kl)]))) % kl]) ^ keys[(i + 39) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 26) % kl])) + (~(keys[(i + 52) % kl]) ^ keys[(i + 13) % kl]) + (keys[(i + 111) % kl] - keys[((i + 143) % kl)] ^ keys[(i + 182) % kl]));

                newmsg[i] = (char) (newmsg[i] ^ keys[(((kl + ml) - i)) % kl]);
                newmsg[i] = (char) (newmsg[i] ^ keys[(i) % kl]);
            }
            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }

    public String decryptSTEP17Y(String message, String key) {
        try {

            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();
            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];
            int kl2 = kl * kl;
            int kml = kl + ml;
            for (int i = 0; i < ml; i++) {
                mesg[i] = (char) (mesg[i] ^ keys[(i) % kl]);
                mesg[i] = (char) (mesg[i] ^ keys[(((kml) - i)) % kl]);
                mesg[i] = (char) (((mesg[i] ^ keys[(i) % kl]) ^ i) ^ (byte) ((kl2) / (i + 1)));
                newmsg[i] = (char) (((mesg[i] - (keys[(i + 153) % kl] - keys[((i + 187) % kl)] ^ keys[(i + 238) % kl]) - (~(keys[(i + 17) % kl] ^ keys[(i + 68) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 34) % kl]) ^ keys[(i + 51) % kl]) - (keys[((kl * (keys[(i + 119) % kl]) ^ (keys[((i + 85) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 111) % kl] - keys[((i + 143) % kl)] ^ keys[(i + 182) % kl]) - (~(keys[(i + 13) % kl] ^ keys[(i + 52) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 26) % kl]) ^ keys[(i + 39) % kl]) - (keys[((kl * (keys[(i + 91) % kl]) ^ (keys[((i + 65) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 99) % kl] - keys[((i + 121) % kl)] ^ keys[(i + 154) % kl]) - (~(keys[(i + 11) % kl] ^ keys[(i + 44) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 22) % kl]) ^ keys[(i + 33) % kl]) - (keys[((kl * (keys[(i + 77) % kl]) ^ (keys[((i + 55) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 63) % kl] - keys[((i + 77) % kl)] ^ keys[(i + 98) % kl]) - (~(keys[(i + 7) % kl] ^ keys[(i + 28) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 14) % kl]) ^ keys[(i + 21) % kl]) - (keys[((kl * (keys[(i + 49) % kl]) ^ (keys[((i + 35) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 45) % kl] - keys[((i + 55) % kl)] ^ keys[(i + 70) % kl]) - (~(keys[(i + 5) % kl] ^ keys[(i + 20) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 10) % kl]) ^ keys[(i + 15) % kl]) - (keys[((kl * (keys[(i + 35) % kl]) ^ (keys[((i + 25) % kl)]))) % kl]));
                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 27) % kl] - keys[((i + 33) % kl)] ^ keys[(i + 42) % kl]) - (~(keys[(i + 3) % kl] ^ keys[(i + 12) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 6) % kl]) ^ keys[(i + 9) % kl]) - (keys[((kl * (keys[(i + 21) % kl]) ^ (keys[((i + 15) % kl)]))) % kl]));

                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 18) % kl] - keys[((i + 22) % kl)] ^ keys[(i + 28) % kl]) - (~(keys[(i + 2) % kl] ^ keys[(i + 8) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 4) % kl]) ^ keys[(i + 6) % kl]) - (keys[((kl * (keys[(i + 14) % kl]) ^ (keys[((i + 10) % kl)]))) % kl]));
                newmsg[i] = (char) (((newmsg[i] - (keys[(i + 9) % kl] - keys[((i + 11) % kl)] ^ keys[(i + 14) % kl]) - (~(keys[(i + 1) % kl] ^ keys[(i + 4) % kl]))) ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 2) % kl]) ^ keys[(i + 3) % kl]) - (keys[((kl * (keys[(i + 7) % kl]) ^ (keys[((i + 5) % kl)]))) % kl]));

            }
            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }

    public String encryptSTEP17Y(String message, String key) {
        try {

            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();
            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];
            int kl2 = kl * kl;
            int kml = kl + ml;
            for (int i = 0; i < ml; i++) {

                newmsg[i] = (char) (((mesg[i] + keys[((kl * (keys[(i + 7) % kl]) ^ (keys[((i + 5) % kl)]))) % kl]) ^ keys[(i + 3) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 2) % kl])) + (~(keys[(i + 4) % kl]) ^ keys[(i + 1) % kl]) + (keys[(i + 9) % kl] - keys[((i + 11) % kl)] ^ keys[(i + 14) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 14) % kl]) ^ (keys[((i + 10) % kl)]))) % kl]) ^ keys[(i + 6) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 4) % kl])) + (~(keys[(i + 8) % kl]) ^ keys[(i + 2) % kl]) + (keys[(i + 18) % kl] - keys[((i + 22) % kl)] ^ keys[(i + 28) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 21) % kl]) ^ (keys[((i + 15) % kl)]))) % kl]) ^ keys[(i + 9) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 6) % kl])) + (~(keys[(i + 12) % kl]) ^ keys[(i + 3) % kl]) + (keys[(i + 27) % kl] - keys[((i + 33) % kl)] ^ keys[(i + 42) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 35) % kl]) ^ (keys[((i + 25) % kl)]))) % kl]) ^ keys[(i + 15) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 10) % kl])) + (~(keys[(i + 20) % kl]) ^ keys[(i + 5) % kl]) + (keys[(i + 45) % kl] - keys[((i + 55) % kl)] ^ keys[(i + 70) % kl]));

                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 49) % kl]) ^ (keys[((i + 35) % kl)]))) % kl]) ^ keys[(i + 21) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 14) % kl])) + (~(keys[(i + 28) % kl]) ^ keys[(i + 7) % kl]) + (keys[(i + 63) % kl] - keys[((i + 77) % kl)] ^ keys[(i + 98) % kl]));

                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 77) % kl]) ^ (keys[((i + 55) % kl)]))) % kl]) ^ keys[(i + 33) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 22) % kl])) + (~(keys[(i + 44) % kl]) ^ keys[(i + 11) % kl]) + (keys[(i + 99) % kl] - keys[((i + 121) % kl)] ^ keys[(i + 154) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 91) % kl]) ^ (keys[((i + 65) % kl)]))) % kl]) ^ keys[(i + 39) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 26) % kl])) + (~(keys[(i + 52) % kl]) ^ keys[(i + 13) % kl]) + (keys[(i + 111) % kl] - keys[((i + 143) % kl)] ^ keys[(i + 182) % kl]));
                newmsg[i] = (char) (((newmsg[i] + keys[((kl * (keys[(i + 119) % kl]) ^ (keys[((i + 85) % kl)]))) % kl]) ^ keys[(i + 51) % kl] ^ (keys[kl - i % kl - 1] + keys[kl - 1] - keys[(i + 34) % kl])) + (~(keys[(i + 68) % kl]) ^ keys[(i + 17) % kl]) + (keys[(i + 153) % kl] - keys[((i + 187) % kl)] ^ keys[(i + 238) % kl]));
                newmsg[i] = (char) (((newmsg[i] ^ keys[(i) % kl]) ^ i) ^ (byte) ((kl2) / (i + 1)));
                newmsg[i] = (char) (newmsg[i] ^ keys[(((kml) - i)) % kl]);
                newmsg[i] = (char) (newmsg[i] ^ keys[(i) % kl]);
            }
            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }

    public String cryptThreedog(String data, boolean decrypt, String key) {
        if (data == null || key == null) return null;
        if (decrypt) {
            data = cryptThreedogDouble(data, true, key);
        }
        if (!decrypt) {
            if (!(data.length() % (BSBYTE) == 0)) {
                String part = data.substring(data.length() - (data.length() % (BSBYTE)));
                String s = " ";
                try {
                    s = threedogBlockFill(part, BSBYTE);
                } catch (Exception e) {
                }
                int i = 0;
                while (!(part.length() == BSBYTE)) {
                    part += s.substring(i, i + 1);
                    i++;
                }
                data = data.substring(0, data.length() - (data.length() % (BSBYTE))) + part;

            }
        }
        byte[] datab;
        if (!decrypt) {
            datab = stringToBytesUTFNIO(data);
        } else {
            datab = stringToByteArray(data);
        }
        int nParts = datab.length / BSBYTE;
        byte[] res = new byte[datab.length];
        byte[] partByte = new byte[BSBYTE];
        byte[] keyb = stringToByteArray(key);
        int kl = keyb.length;
        for (int p = 0; p < nParts; p++) {

            for (int b = 0; b < BSBYTE; b++)
                partByte[b] = datab[p * BSBYTE + b];
            int pkl = p + kl;
            if (!decrypt) {
                for (int i = 0; i < BSBYTE; i++) {
                    partByte[i] = (byte) ((partByte[i] ^ keyb[i % kl]) ^ keyb[(i + 3) % kl]);
                    partByte[i] = (byte) ((partByte[i]) ^ (byte) ((i + pkl) * (p / (i + 1))));
                }
            }
            if (!decrypt)
                partByte = stringToByteArray(encryptSTEP11(byteArrayToString(partByte), key));
            if (decrypt)
                partByte = stringToByteArray(decryptSTEP11(byteArrayToString(partByte), key));
            if (decrypt) {
                for (int i = 0; i < BSBYTE; i++) {
                    partByte[i] = (byte) ((partByte[i] ^ (byte) ((i + pkl) * (p / (i + 1)))));
                    partByte[i] = (byte) ((keyb[(i + 3) % kl] ^ partByte[i]) ^ keyb[i % kl]);
                }
            }
            /*for(int b=0; b<BSBYTE; b++)
                res[p*BSBYTE+b] = partByte[b];*/

            System.arraycopy(partByte, 0, res, p * BSBYTE, BSBYTE);
        }
        if (!decrypt) {
            return cryptThreedogDouble(byteArrayToString(res), false, key);
        } else {
            String str = bytesToStringUTFNIO(res);
            int i = 1;
            try {
                while (!threedogBlockFill(str.substring(str.length() - BSBYTE, str.length() - i), BSBYTE).startsWith(str.substring(str.length() - i)))
                    i++;
                str = str.substring(0, str.length() - i);
            } catch (Exception e) {
            }
            return str;
        }
    }

    public String cryptThreedogDouble(String data, boolean decrypt, String key) {
        if (data == null || key == null) return null;
        if (decrypt) {
            data = cryptThreedogTreble(data, true, key);
        }
        int BSBYTED = (int) (BSBYTE * 1.5);
        if (!(data.length() % BSBYTED == 0)) {
            while (!(data.length() % BSBYTED == 0)) {
                data = data + " ";
            }
        }
        int nParts = data.length() / BSBYTED;
        byte[] res = new byte[data.length()];
        String partStr;
        byte[] partByte;
        byte[] keyb = stringToByteArray(key);
        int kl = keyb.length;
        for (int p = 0; p < nParts; p++) {

            partStr = data.substring(p * BSBYTED, p * BSBYTED + BSBYTED);
            partByte = stringToByteArray(partStr);
            if (!decrypt) {
                for (int i = 0; i < BSBYTE; i++) {
                    partByte[i] = (byte) ((partByte[i] ^ keyb[i % kl]) ^ keyb[(i + 7) % kl]);
                }
            }

            if (!decrypt)
                partByte = stringToByteArray(encryptSTEP13X(byteArrayToString(partByte), key));
            if (decrypt)
                partByte = stringToByteArray(decryptSTEP13X(byteArrayToString(partByte), key));

            if (decrypt) {
                for (int i = 0; i < BSBYTE; i++) {
                    partByte[i] = (byte) ((keyb[(i + 7) % kl] ^ partByte[i]) ^ keyb[i % kl]);
                }
            }
            /*for(int b=0; b<BSBYTED; b++)
                res[p*BSBYTED+b] = partByte[b];*/
            System.arraycopy(partByte, 0, res, p * BSBYTED, BSBYTED);
        }
        if (!decrypt) {
            return cryptThreedogTreble(byteArrayToString(res), false, key);
        } else {
            String str = byteArrayToString(res);
            while (str.endsWith(" ")) {
                str = str.substring(0, str.length() - 1);
            }
            return str;
        }
    }

    public String cryptThreedogTreble(String data, boolean decrypt, String key) {
        if (data == null || key == null) return null;
        int BSBYTED = (int) (BSBYTE * 2.5);
        if (!decrypt) {
            if (!(data.length() % (BSBYTED) == 0)) {
                String part = data.substring(data.length() - (data.length() % (BSBYTED)));
                String s = " ";
                try {
                    s = threedogBlockFill(part, BSBYTED);
                } catch (Exception e) {
                }
                int i = 0;
                while (!(part.length() == BSBYTED)) {
                    part += s.substring(i, i + 1);
                    i++;
                }
                data = data.substring(0, data.length() - (data.length() % (BSBYTED))) + part;

            }
        }
        int nParts = data.length() / BSBYTED;
        byte[] res = new byte[data.length()];
        String partStr;
        byte[] partByte;
        byte[] keyb = stringToByteArray(key);
        int kl = keyb.length;
        for (int p = 0; p < nParts; p++) {

            partStr = data.substring(p * BSBYTED, p * BSBYTED + BSBYTED);
            partByte = stringToByteArray(partStr);
            if (!decrypt) {
                for (int i = 0; i < BSBYTE; i++) {
                    partByte[i] = (byte) ((partByte[i] ^ keyb[i % kl]) ^ keyb[(i + 1) % kl]);
                    partByte[i] = (byte) ((partByte[i]) ^ ((byte) ((i + p) * ((2 * p) + 1 / (i + 1))) ^ i));
                }
            }
            if (!decrypt)
                partByte = stringToByteArray(encryptSTEP17Y(byteArrayToString(partByte), key));
            if (decrypt)
                partByte = stringToByteArray(decryptSTEP17Y(byteArrayToString(partByte), key));
            if (decrypt) {
                for (int i = 0; i < BSBYTE; i++) {
                    partByte[i] = (byte) ((partByte[i] ^ ((byte) ((i + p) * ((2 * p) + 1 / (i + 1))) ^ i)));
                    partByte[i] = (byte) ((keyb[(i + 1) % kl] ^ partByte[i]) ^ keyb[i % kl]);
                }
            }
            /*for(int b=0; b<BSBYTED; b++)
                res[p*BSBYTED+b] = partByte[b];*/
            System.arraycopy(partByte, 0, res, p * BSBYTED, BSBYTED);
        }
        String str = byteArrayToString(res);
        int i = 1;
        try {
            while (!threedogBlockFill(str.substring(str.length() - BSBYTED, str.length() - i), BSBYTED).startsWith(str.substring(str.length() - i))) {
                i++;
            }
            str = str.substring(0, str.length() - i);
        } catch (Exception e) {
        }
        return str;
    }

    public String byteArrayToString(byte[] data) {
        if (data == null) return null;
        StringBuilder sb = new StringBuilder();
        //for (int i = 0; i < data.length; i++) {
        for (byte element : data) {
            int n = element;
            if (n < 0) n += 256;
            sb.append((char) n);
        }
        return sb.toString();

    }

    public byte[] stringToByteArray(String s) {
        if (s == null) return null;
        byte[] temp = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            temp[i] = (byte) s.charAt(i);
        }
        return temp;
    }

    public static byte[] stringToBytesUTFNIO(String str) {
        char[] buffer = str.toCharArray();
        byte[] b = new byte[buffer.length << 1];
        CharBuffer cBuffer = ByteBuffer.wrap(b).asCharBuffer();

        /*for (int i = 0; i < buffer.length; i++)
            cBuffer.put(buffer[i]);*/
        for (char element : buffer) {
            cBuffer.put(element);
        }
        return b;

    }

    public static String bytesToStringUTFNIO(byte[] bytes) {
        CharBuffer cBuffer = ByteBuffer.wrap(bytes).asCharBuffer();
        return cBuffer.toString();
    }


}
