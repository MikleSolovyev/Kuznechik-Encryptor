package core.kuznechik.functions;

public class X {

    public static byte[] direct(byte[] blockIn1, byte[] blockIn2) {
        byte[] blockOut = new byte[Math.min(blockIn1.length, blockIn2.length)];

        for (int i = 0; i < blockOut.length; i++) {
            blockOut[i] = (byte) (blockIn1[i] ^ blockIn2[i]);
        }

        return blockOut;
    }
}
