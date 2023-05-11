package core;

import java.io.*;
import java.security.*;
import java.security.KeyStore.*;
import javax.crypto.*;
import java.util.Base64;

public class KeyDataBase {

    private static final int MasterKeySize = 256;
    private static final int IVSize = 64;

    private static KeyStore keyStore;

    public static void init(String keyFileName, String password) throws Exception {
        File keyFile = new File(keyFileName);
        FileInputStream fis = null;

        if (!keyFile.createNewFile() && keyFile.length() != 0) {
            fis = new FileInputStream(keyFile);
        }

        load(fis, password);

        if (fis != null) {
            fis.close();
        }
    }

    private static void load(InputStream stream, String password) throws Exception {
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try {
            keyStore.load(stream, passwordToHash(password));
        } catch (IOException ex) {
            if (ex.getCause() instanceof UnrecoverableKeyException) {
                throw new UnrecoverableKeyException("Wrong password!");
            }
        }
    }

    public static byte[] read(String alias, String password) throws Exception {
        try {
            return keyStore.getKey(alias, passwordToHash(password)).getEncoded();
        } catch (UnrecoverableKeyException ex) {
            System.out.println("Wrong password!");
        }

        return null;
    }

    public static void write(String alias, String password, String keyFileName) throws Exception {
        SecretKeyEntry entry = new SecretKeyEntry(generateSecretKey());
        ProtectionParameter protParam = new PasswordProtection(passwordToHash(password));

        keyStore.setEntry(alias, entry, protParam);

        FileOutputStream stream = new FileOutputStream(keyFileName);
        keyStore.store(stream, passwordToHash(password));
        stream.close();
    }

    public static boolean containsAlias(String alias) throws Exception {
        return keyStore.containsAlias(alias);
    }

    private static char[] passwordToHash(String password) throws Exception {
        byte[] passHash = MessageDigest.getInstance("SHA-256").digest(password.getBytes());

        return Base64.getEncoder().encodeToString(passHash).toCharArray();
    }

    private static SecretKey generateSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("ARCFOUR");
        //320 bits = 256 bits of masterKey + 64 bits of initVector
        keyGenerator.init(MasterKeySize + IVSize);

        return keyGenerator.generateKey();
    }
}
