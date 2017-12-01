package com.company.messages;

public class Message {

    private MessageType type;
    private byte[] headerField;
    private byte[] typeField;
    private byte[] payloadField;

    public Message(MessageType type, byte[] headerField, byte[] typeField, byte[] payloadField){
        this.type = type;
        this.headerField = headerField;
        this.typeField = typeField;
        this.payloadField = payloadField;
    }

    public byte[] getBytes(){
        int totalMessageLength = headerField.length + typeField.length + payloadField.length;
        int messageIndex = 0;

        // Create the message
        byte[] message = new byte[totalMessageLength];

        // Add the first part of the message
        for (int i = 0; i < headerField.length; i++){
            message[messageIndex] = headerField[i];
            messageIndex++;
        }

        // Add the second part
        for (int i = 0; i < typeField.length; i++){
            message[messageIndex] = typeField[i];
            messageIndex++;
        }

        // Add the third part
        for (int i = 0; i < payloadField.length; i++){
            message[messageIndex] = payloadField[i];
            messageIndex++;
        }

        return message;
    }

    public MessageType getType() {
        return type;
    }

    public byte[] getHeaderField(){
        return headerField;
    }

    public byte[] getPayloadField(){
        return payloadField;
    }
}
