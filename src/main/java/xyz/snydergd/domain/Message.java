package xyz.snydergd.domain;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import xyz.snydergd.exception.InvalidMessageDataException;

public class Message {
    public enum Type {
        OPEN(1),
        CLOSE(2),
        DATA(3);

        private byte code;

        private Type(int code) {
            this.code = (byte)code;
        }
        public byte getCode() {
            return this.code;
        }
        public static Optional<Type> getForCode(byte code) {
            for (Type myType : Type.values()) if (myType.getCode() == code) return Optional.of(myType);
            return Optional.empty();
        }
    }

    private short connectionNumber;
    private Type messageType;
    private byte[] data;

    private static byte[] byteBufferToArray(ByteBuffer data) {
        byte[] dataArray = new byte[data.remaining()];
        data.get(dataArray);
        return dataArray;
    }
    public Message(short connectionNumber, Type messageType, ByteBuffer data) {
        this(connectionNumber, messageType, byteBufferToArray(data));
    }
    public Message(short connectionNumber, Type messageType, byte[] data) {
        this.connectionNumber = connectionNumber;
        this.messageType = messageType;
        this.data = data;
    }

    public static Message fromByteBuffer(ByteBuffer byteBuffer) throws InvalidMessageDataException {
        short length = byteBuffer.getShort();
        if (length < 3) {
            throw new InvalidMessageDataException("Invalid header length -- expecting at least 3: " + length);
        }
        if (length > byteBuffer.remaining()) {
            throw new InvalidMessageDataException("Not enough data remaining in the byte buffer to satisfy the " + length + " bytes specified by length value that came immediately before.");
        }

        short connectionNumber = byteBuffer.getShort();
        byte messageTypeByte = byteBuffer.get();
        Type messageType = Type.getForCode(messageTypeByte).orElseThrow(() -> new InvalidMessageDataException("Message type is invalid: " + messageTypeByte));
        length -= 3;

        // leaving room for future expansion with this.  Ignore it for now
        byteBuffer.position(byteBuffer.position() + length);

        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);

        return new Message(connectionNumber, messageType, data);
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer myReturn = ByteBuffer.allocate(5 + this.data.length);
        myReturn.rewind();
        myReturn.putShort((short)3);
        myReturn.putShort(this.connectionNumber);
        myReturn.put(this.messageType.getCode());
        myReturn.put(this.data);
        myReturn.flip();
        return myReturn;
    }

    public short getConnectionNumber() {
        return connectionNumber;
    }

    public void setConnectionNumber(short connectionNumber) {
        this.connectionNumber = connectionNumber;
    }

    public Type getMessageType() {
        return messageType;
    }

    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    
}
