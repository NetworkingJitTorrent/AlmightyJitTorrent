package com.company.delegates;

import com.company.messages.Message;

import java.util.function.Consumer;

public interface ServerDelegate {

    Message onClientHandshakeReceived(Message message, Consumer<Integer> setClientPeerID);

    Message onClientBitfieldReceived(Message message, int clientPeerID);

    Message onInterestedReceived(Message message, int clientPeerID);

    Message onNotInterestedReceived(Message message, int clientPeerID);

    Message onRequestReceived(Message message, int clientPeerID);
}
