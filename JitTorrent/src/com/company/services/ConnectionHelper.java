package com.company.services;

import com.company.messages.Message;
import com.company.messages.MessageType;

import java.util.Arrays;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

// Lots of code is shared by the Client and Server. This class contains the common
// functionality inherent to both types of connections.
public class ConnectionHelper {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private ConcurrentLinkedQueue<Message> outboundMessageQueue;
    private ConcurrentLinkedQueue<Message> inboundMessageQueue;
    private Thread listenerThread;

    ConnectionHelper(Socket socket){
        this.socket = socket;

        // initialize inputStream and outputStream
        try{
            out = new DataOutputStream(socket.getOutputStream());
            out.flush();
            in = new DataInputStream(socket.getInputStream());
        } catch(Exception e){
            e.printStackTrace();
        }

        // Setup the message queues
        outboundMessageQueue = new ConcurrentLinkedQueue<>();
        inboundMessageQueue = new ConcurrentLinkedQueue<>();
    }

    // Closes the socket
    public void closeConnection() throws Exception{
        in.close();
        out.close();
        socket.close();
    }

    // Adds a message to the outbound message queue
    public void addOutboundMessage(Message m){
        outboundMessageQueue.add(m);
    }

    // Receive inbound messages. On Message Received gets called if there are messages.
    public void receiveInboundMessages(Consumer<Message> onMessageReceived){
        // Check the input buffer for stuff yo
        while(!inboundMessageQueue.isEmpty()){
            onMessageReceived.accept(inboundMessageQueue.poll());
        }
    }

    public void listenForMessages(){
        // If we don't have a thread listening for incoming messages start one dawg.
        if (listenerThread == null || !listenerThread.isAlive()){
            listenerThread = new Thread(() -> {
                try {
                    int length = in.readInt();
                    byte[] request = new byte[length];
                    in.readFully(request);
                    Message newMessage = MessageType.createMessageFromByteArray(request);
                    inboundMessageQueue.add(newMessage);
                } catch (EOFException e){
                    //DO NOTHING!!!
                } catch (Exception e){
                    e.printStackTrace();
                }
            });
            listenerThread.start();
        }
    }

    // Sends a message to the output stream
    public void sendOutboundMessages()
    {
        while(!outboundMessageQueue.isEmpty()){

            Message msg = outboundMessageQueue.poll();
            try{
                //stream write the message
                out.writeInt(msg.getBytes().length);
                out.write(msg.getBytes());
                out.flush();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
}
