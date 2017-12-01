package com.company.delegates;

import com.company.messages.Message;

public interface ClientDelegate {

    Message onServerHandshakeReceived(Message message, int serverPeerID);

    Message onServerBitfieldReceived(Message message, int serverPeerID);

    Message onChokeReceived(Message message, int serverPeerID);

    Message onUnChokeReceived(Message message, int serverPeerID);

    Message onHaveReceived(Message message, int serverPeerID);

    Message onPieceReceived(Message message, int serverPeerID);
}
