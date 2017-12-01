package com.company.messages;

import java.nio.ByteBuffer;

public class ActualMessage extends Message {
    public ActualMessage(MessageType type, byte[] headerField, byte[] typeField, byte[] payloadField) {
        super(type, headerField, typeField, payloadField);
    }

    public int getMessageLength(){
        return ByteBuffer.wrap(getHeaderField()).getInt(); // TODO: Verify this is the same as payload.length();
    }
}
