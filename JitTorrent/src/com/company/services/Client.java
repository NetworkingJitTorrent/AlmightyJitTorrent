package com.company.services;

import com.company.configs.PeerInfo;
import com.company.delegates.ClientDelegate;
import com.company.messages.Message;
import com.company.messages.MessageType;

import java.net.Socket;
import java.nio.ByteBuffer;

public class Client {
    private int peerID;
    private int serverPeerID;
    private ClientDelegate delegate;
    private ConnectionHelper connectionHelper;

    public Client(int peerID, ClientDelegate delegate){
        this.peerID = peerID;
        this.delegate = delegate;
    }

    public void startConnection(PeerInfo peerInfo) throws Exception
    {
        // Set the server peerID
        serverPeerID = peerInfo.getPeerID();

        //create a socket to connect to the server
        Socket socket = new Socket(peerInfo.getHostName(), peerInfo.getListeningPort());

        // Create our helper
        connectionHelper = new ConnectionHelper(socket);

        // Add the Handshake message as an outbound message
        byte[] handshakePayload = ByteBuffer.allocate(4).putInt(peerID).array();
        Message handshake = MessageType.HANDSHAKE.createMessageFromPayload(handshakePayload);
        addOutboundMessage(handshake);

        // Start loopin brah
        while (true)
        {
            // Send any outbound messages
            connectionHelper.sendOutboundMessages();

            // Listen on the input port
            connectionHelper.listenForMessages();

            // Read any inbound messages
            connectionHelper.receiveInboundMessages(this::notifyDelegate);
        }
    }

    // Closes the connection
    public void closeConnection() throws Exception{
        connectionHelper.closeConnection();
    }

    // Adds an outbound message to the connection
    public void addOutboundMessage(Message m){
        connectionHelper.addOutboundMessage(m);
    }

    private void notifyDelegate(Message message){

        Message m;
        switch(message.getType()){
            case HANDSHAKE:
                m = delegate.onServerHandshakeReceived(message, serverPeerID);
                break;
            case BITFIELD:
                m = delegate.onServerBitfieldReceived(message, serverPeerID);
                break;
            case CHOKE:
                m = delegate.onChokeReceived(message, serverPeerID);
                break;
            case UNCHOKE:
                m = delegate.onUnChokeReceived(message, serverPeerID);
                break;
            case HAVE:
                m = delegate.onHaveReceived(message, serverPeerID);
                break;
            case PIECE:
                m = delegate.onPieceReceived(message, serverPeerID);
                break;
            default:
                m = null;
        }

        if (m != null){
            addOutboundMessage(m);
        }
    }
}
