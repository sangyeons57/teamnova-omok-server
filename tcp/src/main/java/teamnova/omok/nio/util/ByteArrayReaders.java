package teamnova.omok.nio.util;

/**
 * Utility helpers for reading network-order integers from byte arrays.
 */
public final class ByteArrayReaders {
    private ByteArrayReaders() {
    }

    public static int readIntBE(byte[] buffer, int offset, int availableLength) {
        requireRange(buffer, offset, 4, availableLength);
        return ((buffer[offset] & 0xFF) << 24)
                | ((buffer[offset + 1] & 0xFF) << 16)
                | ((buffer[offset + 2] & 0xFF) << 8)
                | (buffer[offset + 3] & 0xFF);
    }

    public static long readUnsignedIntBE(byte[] buffer, int offset, int availableLength) {
        return readIntBE(buffer, offset, availableLength) & 0xFFFF_FFFFL;
    }

    private static void requireRange(byte[] buffer, int offset, int length, int availableLength) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer is null");
        }
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("offset and length must be non-negative");
        }
        if (offset + length > availableLength) {
            throw new IllegalArgumentException("Requested range exceeds available bytes");
        }
    }
}
