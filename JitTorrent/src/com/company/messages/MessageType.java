package com.company.messages;

import java.nio.ByteBuffer;

// This is more of a MessageFactory probably but thats okay.
public enum MessageType {
    CHOKE,
    UNCHOKE,
    INTERESTED,
    NOT_INTERESTED,
    HAVE,
    BITFIELD,
    REQUEST,
    PIECE,
    HANDSHAKE;

    private final static String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
    private final static int ZERO_BIT_SIZE = 10;
    private final static int HANDSHAKE_PAYLOAD_SIZE = 4;
    private final static int MESSAGE_LENGTH_SIZE = 4;
    private final static int MESSAGE_TYPE_SIZE = 1;

    // Creates a message with the given payload. Use this when creating a message to send.
    public Message createMessageFromPayload(byte[] payload){

        if (this == MessageType.HANDSHAKE){
            byte[] header = HANDSHAKE_HEADER.getBytes();
            byte[] zeroBits = new byte[ZERO_BIT_SIZE];

            return new HandshakeMessage(this, header, zeroBits, payload);

        } else{
            byte[] messageLength = ByteBuffer.allocate(4).putInt(payload.length).array();
            byte[] typeFourByte = ByteBuffer.allocate(4).putInt(this.ordinal()).array(); // TODO: Verify that this works correclty
            byte[] type = new byte[MESSAGE_TYPE_SIZE];
            type[0] = typeFourByte[3];
            return new ActualMessage(this, messageLength, type, payload);
        }
    }

    // Creates a message from a byte array. Use this when receiving a message and you want to convert the byte stream to an object.
    public static Message createMessageFromByteArray(byte[] data){

        // Determine if this is a handshake message or an actual message
        if (data.length >= HANDSHAKE_HEADER.length() &&
                HANDSHAKE_HEADER.equals(new String(data, 0, HANDSHAKE_HEADER.length()))){

            // Create a handshake message

            // Create the header
            byte[] headerBytes = HANDSHAKE_HEADER.getBytes();

            // Create zero bits
            byte[] zeroBits = new byte[ZERO_BIT_SIZE];

            // Create the payload (peerID)
            byte[] payload = new byte[HANDSHAKE_PAYLOAD_SIZE];
            int payloadStartIndex = HANDSHAKE_HEADER.length() + ZERO_BIT_SIZE;

            System.arraycopy(data, payloadStartIndex,  payload,0, HANDSHAKE_PAYLOAD_SIZE);


            return new HandshakeMessage(MessageType.HANDSHAKE, headerBytes, zeroBits, payload);
        } else{

            // Create a regular message

            // Determine the message length
            byte[] messageLength = new byte[MESSAGE_LENGTH_SIZE];
            System.arraycopy(data, 0, messageLength, 0, MESSAGE_LENGTH_SIZE);

            // Determine the message type
            byte[] messageType = new byte[MESSAGE_TYPE_SIZE];
            System.arraycopy(data, MESSAGE_LENGTH_SIZE, messageType, 0, MESSAGE_TYPE_SIZE);

            // Determine the message Payload
            int messageLengthInt = ByteBuffer.wrap(messageLength).getInt();
            byte[] payload = new byte[messageLengthInt];
            System.arraycopy(data, MESSAGE_LENGTH_SIZE + MESSAGE_TYPE_SIZE, payload, 0, messageLengthInt);

            // Determine the enum type
            byte[] typeFourByte = new byte[4];
            typeFourByte[3] = messageType[0];
            MessageType type = MessageType.values()[ByteBuffer.wrap(typeFourByte).getInt()];

            return new ActualMessage(type, messageLength, messageType, payload);
        }
    }
}
