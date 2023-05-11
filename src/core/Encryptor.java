package core;

import core.kuznechik.KeyExpander;
import core.kuznechik.functions.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Encryptor {

    private static final int masterKeyLength = 32;
    private static final int initVectorLength = 16;
    private static final int numberOfIterationKeys = 10;
    //256 recommended (Р 1323565.1.017—2018 Приложение В (рекомендуемое))
    private static final long frequencyOfACPKM = 2;
    private static final byte[][] constantD = {
            {
                    (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87,
                    (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x8B, (byte) 0x8C, (byte) 0x8D, (byte) 0x8E, (byte) 0x8F
            },
            {
                    (byte) 0x90, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97,
                    (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0x9B, (byte) 0x9C, (byte) 0x9D, (byte) 0x9E, (byte) 0x9F
            }
    };

    private static byte[][] masterKey;
    private static byte[] initVector;
    private static byte[][] roundKeys;
    private static long ctrCounter = 0;

    public static void setMasterKey(byte[][] masterKey) {
        Encryptor.masterKey = masterKey;
        Encryptor.roundKeys = new byte[numberOfIterationKeys][masterKeyLength / 2];

        KeyExpander.expandKey(roundKeys, masterKey);
    }

    public static void setInitVector(byte[] initVector) {
        Encryptor.initVector = Arrays.copyOf(initVector, initVectorLength);
    }

    //Only for tests of CTR-ACPKM mode when you need to decrypt after encryption
    private static void setCtrCounter(long ctrCounter) {
        Encryptor.ctrCounter = ctrCounter;
    }

    public static void parseSecretKey(byte[] secretKey) {
        setMasterKey(new byte[][]{Arrays.copyOfRange(secretKey, 0, masterKeyLength / 2),
                Arrays.copyOfRange(secretKey, masterKeyLength / 2, masterKeyLength)});

        setInitVector(Arrays.copyOfRange(secretKey, masterKeyLength, secretKey.length));
    }

    public static byte[] encryptWithNoMode(byte[] blockIn) {
        byte[] blockOut = Arrays.copyOf(blockIn, blockIn.length);

        for (int i = 0; i < roundKeys.length - 1; i++) {
            blockOut = L.direct(S.direct(X.direct(blockOut, roundKeys[i])));
        }

        blockOut = X.direct(blockOut, roundKeys[roundKeys.length - 1]);

        return blockOut;
    }

    public static byte[] decryptWithNoMode(byte[] blockIn) {
        byte[] blockOut = Arrays.copyOf(blockIn, blockIn.length);

        for (int i = roundKeys.length - 1; i > 0; i--) {
            blockOut = S.reverse(L.reverse(X.direct(blockOut, roundKeys[i])));
        }

        blockOut = X.direct(blockOut, roundKeys[0]);

        return blockOut;
    }

    //This method also decrypt using CTR-ACPKM mode
    public static byte[] encryptWithCTRACPKM(byte[] blockIn) {
        if (ctrCounter > 0L && ctrCounter % frequencyOfACPKM == 0L) {
            acpkm();
        }

        ctr();

        return X.direct(blockIn, encryptWithNoMode(initVector));
    }

    private static void ctr() {
        System.arraycopy(
                ByteBuffer.allocate(initVector.length / 2)
                        .putLong(0, ctrCounter).array(), 0,
                initVector, initVector.length / 2,
                initVector.length / 2);

        ctrCounter++;
    }

    private static void acpkm() {
        byte[] firstKey = encryptWithNoMode(constantD[0]);
        byte[] secondKey = encryptWithNoMode(constantD[1]);

        setMasterKey(new byte[][]{firstKey, secondKey});
    }

    //Test encryption/decryption with no mode (ГОСТ Р 34.12─2015 Приложение А.1.5 и А.1.6)
    public static void testEncryptDecryptWithNoMode() {
        byte[][] key = {
                {
                        (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF,
                        (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77
                },
                {
                        (byte) 0xFE, (byte) 0xDC, (byte) 0xBA, (byte) 0x98, (byte) 0x76, (byte) 0x54, (byte) 0x32, (byte) 0x10,
                        (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
                }
        };

        byte[] plainText = {
                (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x00,
                (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA, (byte) 0x99, (byte) 0x88
        };

        Encryptor.setMasterKey(key);

        System.out.println("Test encryption/decryption with no mode (ГОСТ Р 34.12─2015 Приложение А.1.5 и А.1.6)");
        System.out.println("\tInput: " + Arrays.toString(plainText));

        plainText = Encryptor.encryptWithNoMode(plainText);
        System.out.println("\tEncrypted: " + Arrays.toString(plainText));

        plainText = Encryptor.decryptWithNoMode(plainText);
        System.out.println("\tDecrypted: " + Arrays.toString(plainText));

        System.out.println("\n");
    }

    //Test encryption/decryption with CTR-ACPKM mode (Р 1323565.1.017—2018 Приложение А.2)
    public static void testEncryptDecryptCTRACPKM() {
        byte[][] key = {
                {
                        (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF,
                        (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77
                },
                {
                        (byte) 0xFE, (byte) 0xDC, (byte) 0xBA, (byte) 0x98, (byte) 0x76, (byte) 0x54, (byte) 0x32, (byte) 0x10,
                        (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
                }
        };

        byte[] iv = {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0xAB, (byte) 0xCE, (byte) 0xF0};

        byte[][] txt = {
                {
                        (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x00, (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA, (byte) 0x99, (byte) 0x88
                },
                {
                        (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xEE, (byte) 0xFF, (byte) 0x0A
                },
                {
                        (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xEE, (byte) 0xFF, (byte) 0x0A, (byte) 0x00
                },
                {
                        (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xEE, (byte) 0xFF, (byte) 0x0A, (byte) 0x00, (byte) 0x11
                },
                {
                        (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xEE, (byte) 0xFF, (byte) 0x0A, (byte) 0x00, (byte) 0x11, (byte) 0x22
                },
                {
                        (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xEE, (byte) 0xFF, (byte) 0x0A, (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33
                },
                {
                        (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xEE, (byte) 0xFF, (byte) 0x0A, (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44
                }
        };

        Encryptor.setMasterKey(key);
        Encryptor.setInitVector(iv);

        System.out.println("Test encryption/decryption with CTR-ACPKM mode (Р 1323565.1.017—2018 Приложение А.2)");

        System.out.println("\tInput:");
        for (int i = 0; i < txt.length; i++) {
            System.out.println("\t" + Arrays.toString(txt[i]));
        }

        System.out.println("\n\tEncrypted:");
        for (int i = 0; i < txt.length; i++) {
            txt[i] = Encryptor.encryptWithCTRACPKM(txt[i]);
            System.out.println("\t" + Arrays.toString(txt[i]));
        }

        Encryptor.setMasterKey(key);
        Encryptor.setCtrCounter(0);

        System.out.println("\n\tDecrypted:");
        for (int i = 0; i < txt.length; i++) {
            txt[i] = Encryptor.encryptWithCTRACPKM(txt[i]);
            System.out.println("\t" + Arrays.toString(txt[i]));
        }
    }
}
