import core.*;

import java.io.File;
import java.security.UnrecoverableKeyException;

public class Main {

    private static final int sizeOfBuffer = 16;

    public static void main(String[] args) throws Exception {
        //Parse arguments
        if (!IOController.parseArguments(args)) {
            return;
        }

        //Run tests from GOST if needed
        if (IOController.isTestMode()) {
            TestsFromGOST.run();
            return;
        }

        //Check password and initialize KeyDataBase
        try {
            KeyDataBase.init(IOController.getKeyFileName(), IOController.getPassword());
        } catch (UnrecoverableKeyException ex) {
            System.out.println(ex.getMessage());

            return;
        }

        //Generate secret key (master key + init vector) in KeyDataBase if needed
        if (!KeyDataBase.containsAlias("0")) {
            KeyDataBase.write("0", IOController.getPassword(), IOController.getKeyFileName());
        }

        //Read master key and init vector from KeyDataBase
        byte[] secretKey = KeyDataBase.read("0", IOController.getPassword());
        Encryptor.parseSecretKey(secretKey);

        //Configure input/output files
        File fileIn = IOController.joinFiles(IOController.getInputFileNames());
        File fileOut = new File(IOController.getOutputFileName());

        //Set input and output files
        IOController.setInOutFiles(fileIn, fileOut);

        //Read all bytes block by block from input file, encrypt/decrypt them and write to output file
        byte[] buffer = IOController.readFromFileToBuffer(sizeOfBuffer);
        while (buffer != null) {
            IOController.writeToFileFromBuffer(Encryptor.encryptWithCTRACPKM(buffer));
            buffer = IOController.readFromFileToBuffer(sizeOfBuffer);
        }

        IOController.closeInOutFiles();
    }
}
