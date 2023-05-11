package core;

import java.io.*;
import java.util.Arrays;
import java.util.zip.*;

class WrongArgumentsException extends Exception {

    public String getMessage() {
        String firstMessage = """
                To run tests from GOST use only one argument -t.
                                
                Usage: [key] <key> [password] <password> [input] <input file/files> [output] <output file>

                """;

        String secondMessage = """
                All arguments are mandatory and must be strictly ordered.

                \t1. -k, path to key
                \t2. -p, String password
                \t3. -i, paths to input files/directory(-ies) (only encryption) or path to input file (encryption/decryption)
                \t4. -o, path to output file""";

        return firstMessage + secondMessage;
    }
}

public class IOController {

    private static boolean testMode;
    private static String keyFileName;
    private static String password;
    private static String[] inputFileNames;
    private static String outputFileName;

    private static RandomAccessFile srcFile;
    private static RandomAccessFile destFile;

    public static boolean isTestMode() {
        return testMode;
    }

    public static String getKeyFileName() {
        return keyFileName;
    }

    public static String getPassword() {
        return password;
    }

    public static String[] getInputFileNames() {
        return inputFileNames;
    }

    public static String getOutputFileName() {
        return outputFileName;
    }

    public static boolean parseArguments(String[] args) {
        try {
            if (!(args != null && (args.length == 1 && args[0].equals("-t")
                    || args.length >= 8
                    && args[0].equals("-k")
                    && args[2].equals("-p")
                    && args[4].equals("-i")
                    && args[args.length - 2].equals("-o")))) {
                throw new WrongArgumentsException();
            }

            if (args[0].equals("-t")) {
                testMode = true;
            } else {
                testMode = false;
                keyFileName = args[1];
                password = args[3];
                inputFileNames = Arrays.copyOfRange(args, 5, args.length - 2);
                outputFileName = args[args.length - 1];
            }
        } catch (WrongArgumentsException ex) {
            System.out.println(ex.getMessage());
            return false;
        }

        return true;
    }

    public static File joinFiles(String[] fileNames) throws Exception {
        File file = new File(fileNames[0]);

        if (fileNames.length == 1 && !file.isDirectory()) {
            return file;
        } else {
            return addToZip(fileNames);
        }
    }

    private static File addToZip(String[] fileNames) throws Exception {
        File zipFile = new File(outputFileName);
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));

        for (String fileName : fileNames) {
            File file = new File(fileName);
            addFileToZipRecursive(zipOut, file);
        }

        zipOut.close();

        return zipFile;
    }

    private static void addFileToZipRecursive(ZipOutputStream zipOut, File file) throws Exception {
        if (!file.isDirectory()) {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[fis.available()];

            if (fis.available() != fis.read(buffer)) {
                throw new Exception("Can`t read all bytes from " + file.getPath() + "!");
            }

            fis.close();

            ZipEntry entry = new ZipEntry(file.getPath());
            zipOut.putNextEntry(entry);
            zipOut.write(buffer);
            zipOut.closeEntry();
        } else {
            File[] listOfFiles = file.listFiles();

            if (listOfFiles == null) {
                throw new Exception(file.getPath() + " is broken directory!");
            } else if (listOfFiles.length > 0) {
                for (File f : listOfFiles) {
                    addFileToZipRecursive(zipOut, f);
                }
            } else {
                ZipEntry entry = new ZipEntry(file.getPath() + "/");
                zipOut.putNextEntry(entry);
                zipOut.closeEntry();
            }
        }
    }

    public static void setInOutFiles(File fileIn, File fileOut) throws Exception {
        if (fileIn.getAbsolutePath().equals(fileOut.getAbsolutePath())) {
            srcFile = destFile = new RandomAccessFile(fileIn, "rw");
        } else {
            srcFile = new RandomAccessFile(fileIn, "r");
            destFile = new RandomAccessFile(fileOut, "rw");
        }
    }

    public static byte[] readFromFileToBuffer(int sizeOfBuffer) throws Exception {
        byte[] buffer = new byte[sizeOfBuffer];

        int newSize = srcFile.read(buffer);

        if (newSize == -1) {
            return null;
        } else if (newSize != sizeOfBuffer) {
            buffer = Arrays.copyOf(buffer, newSize);
        }

        return buffer;
    }

    public static void writeToFileFromBuffer(byte[] buffer) throws Exception {
        if (destFile == srcFile) {
            destFile.seek(destFile.getFilePointer() - buffer.length);
            destFile.write(buffer);
        } else {
            destFile.write(buffer);
        }
    }

    public static void closeInOutFiles() throws Exception {
        if (srcFile != null) {
            srcFile.close();
        }

        if (destFile != null && destFile != srcFile) {
            destFile.close();
        }
    }
}
