package core;

import core.kuznechik.KeyExpander;
import core.kuznechik.functions.*;

public class TestsFromGOST {

    /*
    Method for testing separate functions from "ГОСТ Р 34.12─2015 Приложение А.1"
    and "Р 1323565.1.017—2018 Приложение А.2"
     */
    public static void run() {
        //Test S (ГОСТ Р 34.12─2015 Приложение А.1.1)
        S.testSelf();

        //Test R (ГОСТ Р 34.12─2015 Приложение А.1.2)
        L.testR();

        //Test L (ГОСТ Р 34.12─2015 Приложение А.1.3)
        L.testSelf();

        //Test key expand (ГОСТ Р 34.12─2015 Приложение А.1.4)
        KeyExpander.testSelf();

        //Test encryption/decryption with no mode (ГОСТ Р 34.12─2015 Приложение А.1.5 и А.1.6)
        Encryptor.testEncryptDecryptWithNoMode();

        //Test encryption/decryption with CTR-ACPKM mode (Р 1323565.1.017—2018 Приложение А.2)
        Encryptor.testEncryptDecryptCTRACPKM();
    }
}
