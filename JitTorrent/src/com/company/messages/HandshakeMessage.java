package com.company.messages;

import java.nio.ByteBuffer;

// This class is just to wrap some of the properties with getters to make it easier to use.
public class HandshakeMessage extends Message {
    public HandshakeMessage(MessageType type, byte[] headerField, byte[] typeField, byte[] payloadField) {
        super(type, headerField, typeField, payloadField);
    }

    public int getPeerID(){
        return ByteBuffer.wrap(getPayloadField()).getInt();
    }

    public String getHandshakeHeader(){
        return new String(getHeaderField(), 0, getHeaderField().length);
    }
}
