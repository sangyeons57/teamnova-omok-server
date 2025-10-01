package teamnova.omok.nio.codec;

import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.util.ByteArrayReaders;

/**
 * Decodes binary frames that follow the {@link FrameFormat} contract.
 */
public final class DecodeFrame {
    /**
     * Attempts to decode a frame from the provided buffer.
     *
     * @param buffer   rolling read buffer containing received bytes
     * @param available number of valid bytes currently stored in {@code buffer}
     * @return decoded frame and consumed byte count, or {@code null} if more bytes are required
     * @throws FrameDecodeException if the frame size is invalid or exceeds the allowed maximum
     */
    public static Result tryDecode(byte[] buffer, int available) throws FrameDecodeException {
        if (available < FrameFormat.HEADER_LENGTH) {
            return null;
        }

        int totalLength;
        try {
            totalLength = ByteArrayReaders.readIntBE(buffer, 0, available);
        } catch (IllegalArgumentException e) {
            throw new FrameDecodeException("Unable to read frame length", e);
        }
        if (totalLength < FrameFormat.HEADER_LENGTH) {
            throw new FrameDecodeException("Frame length " + totalLength + " smaller than header");
        }
        if (totalLength > FrameFormat.MAX_FRAME_SIZE) {
            throw new FrameDecodeException("Frame length " + totalLength + " exceeds maximum " + FrameFormat.MAX_FRAME_SIZE);
        }

        if (available < totalLength) {
            return null;
        }

        byte type = buffer[FrameFormat.LENGTH_FIELD_SIZE];
        long requestId;
        try {
            requestId = ByteArrayReaders.readUnsignedIntBE(buffer,
                    FrameFormat.LENGTH_FIELD_SIZE + FrameFormat.TYPE_FIELD_SIZE,
                    available);
        } catch (IllegalArgumentException e) {
            throw new FrameDecodeException("Unable to read request id", e);
        }

        int payloadLength = totalLength - FrameFormat.HEADER_LENGTH;
        byte[] payload = new byte[payloadLength];
        if (payloadLength > 0) {
            System.arraycopy(buffer, FrameFormat.HEADER_LENGTH, payload, 0, payloadLength);
        }

        FramedMessage frame = new FramedMessage(type, requestId, payload);
        return new Result(frame, totalLength);
    }

    public static final class Result {
        private final FramedMessage frame;
        private final int bytesConsumed;

        Result(FramedMessage frame, int bytesConsumed) {
            this.frame = frame;
            this.bytesConsumed = bytesConsumed;
        }

        public FramedMessage frame() {
            return frame;
        }

        public int bytesConsumed() {
            return bytesConsumed;
        }
    }

    public static class FrameDecodeException extends Exception {
        public FrameDecodeException(String message) {
            super(message);
        }

        public FrameDecodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
