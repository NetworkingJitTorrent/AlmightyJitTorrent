import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Peer implements ClientDelegate, ServerDelegate {

    // Private Members
    private int peerID;
    private CommonConfig commonConfig;
    private BitSet bitField;
    private int numPieces;
    private InMemoryFile inMemFile;
    private HashMap<Integer, BitSet> serverBitfields;
    private HashMap<Integer, PeerInfo> peerInfo;
    private HashMap<Integer, Client> clientConnections;
    private Server server;
    private HashSet<Integer> interestedNeighbors;
    private HashMap<Integer, Integer> serverDataTransferTrackingMap;
    private HashSet<Integer> preferredNeighbors;
    private Integer optimisticallyUnchokedNeighbor;
    private HashSet<Integer> handshakedPeers;

    // TODO: We should probably eventually remove clients once the connection terminates. Look into this once we
    // TODO: get to that point (when a peer has a complete file it no longer needs to be a client of anything so
    // TODO: we should terminate those connections).

    Peer(int peerID, CommonConfig commonConfig){
        this.peerID = peerID;
        this.commonConfig = commonConfig;

        // Initialize the bitField
        int fileSize = commonConfig.getFileSize();
        int pieceSize = commonConfig.getPieceSize();
        this.numPieces = fileSize / pieceSize + (fileSize % pieceSize == 0 ? 0 : 1);
        this.bitField = new BitSet(numPieces);

        // Initialize an empty file in memory.
        this.inMemFile = new InMemoryFile(new byte[fileSize], pieceSize);

        // Initialize the hashmap for tracking your server friends
        this.serverBitfields = new HashMap<>();

        // Initialize connection variables
        this.clientConnections = new HashMap<>();
        this.peerInfo = new HashMap<>();

        // Initialize neighbor-choking/unchoking variables
        interestedNeighbors = new HashSet<>();
        serverDataTransferTrackingMap = new HashMap<>();
        preferredNeighbors = new HashSet<>();
        optimisticallyUnchokedNeighbor = null;

        handshakedPeers = new HashSet<>();
    }

    public void start(List<PeerInfo> peerInfo) throws Exception{

        // Search through peerInfo for my peerID
        int index = 0;
        while (index < peerInfo.size() && peerInfo.get(index).getPeerID() != peerID){
            index++;
        }

        // Verify that we have found the peerID
        if (index == peerInfo.size()){
            throw new Exception("PeerID: " + peerID + " not found in PeerInfo.");
        }

        // Get the PeerInfo corresponding to this peer.
        PeerInfo myInfo = peerInfo.get(index);

        // If the peer has the file, set the bitfield and load the file into memory.
        if (myInfo.getHasFile()){
            bitField.set(0, numPieces);

            // Load the file into memory
            this.inMemFile = InMemoryFile.createFromFile(getFilePath(), commonConfig.getPieceSize());
        }

        // Start the server.
        startServer(myInfo.getListeningPort());

        // Make connections to all peers  that came before this one in the PeerInfo list.
        for (int i = 0; i < index; i++){
            startClientConnection(peerInfo.get(i));
        }

        // For later use create a map of peerInfo (will be needed later on to initiate client connections)
        for (int i = 0; i < peerInfo.size(); i++){
            this.peerInfo.put(peerInfo.get(i).getPeerID(), peerInfo.get(i));
        }

        Timer determinePreferredNeighborsTimer = new Timer();
        determinePreferredNeighborsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                determinePreferredNeighbors();
            }
        }, 0, 1000 * commonConfig.getUnchokingInterval());

        Timer determineOptimisticallyUnchokedNeighborTimer = new Timer();
        determineOptimisticallyUnchokedNeighborTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                determineOptimisticallyUnchokedNeighbor();
            }
        }, 0, 1000 * commonConfig.getOptimisticUnchokingInterval());
    }

    // Helper Methods
    private String getFilePath(){
        return "./peer_" + peerID + "/" + commonConfig.getFileName();
    }

    private void startServer(int port){
        server = new Server(this);
        new Thread(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                e.printStackTrace();
            } finally{
                try {
                    server.closeConnection();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    private void startClientConnection(PeerInfo serverInfo){
        Client client = new Client(peerID, this);

        clientConnections.put(serverInfo.getPeerID(), client);
        new Thread(() -> {
            try {
                client.startConnection(serverInfo);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    client.closeConnection();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    private void sendMessageToAllServers(Message message) {
        clientConnections.forEach((key, client) -> {
            client.addOutboundMessage(message);
        });
    }

    private void writeFileToMemory() {
        try {
            String fileName = peerID + commonConfig.getFileName();
            inMemFile.writeToFileWithPeerID(fileName, peerID);

            // need to log the event
            String messageToLog =  "Peer " + peerID + " has downloaded the complete file.";
            JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

            // if everyone is done then exit
            // we need this because since it can run on remote machines exit(0) is local to one system
            ifEveryoneIsDoneExit();
        } catch (Exception e) {
            System.out.println("client: " + peerID + " finished but could not print to file");
        }
    }

    private void ifEveryoneIsDoneExit() {
        boolean allAreDone = true;
        for (BitSet subServerBitSet : serverBitfields.values()) {
            if (!inMemFile.isFinished(subServerBitSet, numPieces)) {
                allAreDone = false;
                break;
            }
        }
        if (allAreDone) {
            System.out.println("All clients have downloaded the file, so I am terminating!");
            // wait a little bit to account for delayed messages and then close the client connections
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            }).start();
        }
    }

    // Timer Methods
    private void determinePreferredNeighbors() {
        // keep reference to old preferred neighbors
        ArrayList<Integer> oldPreferredNeighbors = new ArrayList<>(preferredNeighbors);

        // if file is downloaded then chooses randos from interested neighbors
        // else it should consider the download rate
        if (inMemFile.isFinished(bitField, numPieces)) {
            // clear preferred neighbors and determine new preferred neighbors from interested
            preferredNeighbors.clear();

            if (interestedNeighbors.size() <= commonConfig.getNumberOfPreferredNeighbors()) {
                preferredNeighbors = new HashSet<>(interestedNeighbors);
            }
            else {
                ArrayList<Integer> copyOfInterestedNeighbors = new ArrayList<>(interestedNeighbors);
                int remainingNeighbors = commonConfig.getNumberOfPreferredNeighbors();
                while (remainingNeighbors > 0) {
                    int randoNumber = ThreadLocalRandom.current().nextInt(0, copyOfInterestedNeighbors.size());
                    preferredNeighbors.add(copyOfInterestedNeighbors.get(randoNumber));
                    remainingNeighbors = remainingNeighbors - 1;
                    // remove from that index so we don't repeat randos
                    copyOfInterestedNeighbors.remove(randoNumber);
                }
            }
        }
        else {
            // determine top download peers
            TopDownloadPriorityQueue topDownloadPeers = new TopDownloadPriorityQueue();

            serverDataTransferTrackingMap.forEach((key, value) -> {
                ClientDataStream cds = new ClientDataStream(key, value, commonConfig.getUnchokingInterval());
                topDownloadPeers.add(cds);
            });

            // clear preferredNeighbors so we can update with the new preferred neighbors
            preferredNeighbors.clear();
            int neighborsToAdd = commonConfig.getNumberOfPreferredNeighbors();
            while (!topDownloadPeers.isEmpty() && neighborsToAdd > 0) {
                ClientDataStream cds = topDownloadPeers.poll();
                if (interestedNeighbors.contains(cds.id)) {
                    preferredNeighbors.add(cds.id);
                    neighborsToAdd = neighborsToAdd - 1;
                }
            }

            // reset data transfer map to zeros
            clientConnections.forEach((key, client) -> {
                serverDataTransferTrackingMap.put(key, 0);
            });
        }

        // determine everyone to send a choke message to and send them
        ArrayList<Integer> chokedClientIDs = new ArrayList<>();
        for (Integer oldPreferredNeighbor : oldPreferredNeighbors) {
            // we only send a choke to someone we originally unchoked because everyone choked can stay choked
            if (!preferredNeighbors.contains(oldPreferredNeighbor)) {
                chokedClientIDs.add(oldPreferredNeighbor);
            }
        }
        Message chokeMessage = MessageType.CHOKE.createMessageFromPayload(new byte[]{});
        this.server.sendMessageToListOfClients(chokedClientIDs, chokeMessage);

        // determine everyone to send an unchoke message to and send them
        ArrayList<Integer> unchokedClientIDs = new ArrayList<>();
        for (Integer preferredNeighbor : preferredNeighbors) {
            // we only send an unchoke to someone we originally choked because everyone unchoked can stay unchoked
            if (!oldPreferredNeighbors.contains(preferredNeighbor)) {
                unchokedClientIDs.add(preferredNeighbor);
            }
        }
        Message unchokeMessage = MessageType.UNCHOKE.createMessageFromPayload(new byte[]{});
        this.server.sendMessageToListOfClients(unchokedClientIDs, unchokeMessage);

        // need to log the event
        if (preferredNeighbors.size() > 0) {
            StringBuilder preferredNeighborsSB = new StringBuilder();
            int i = 0;
            for (Integer neighbor : preferredNeighbors) {
                preferredNeighborsSB.append(neighbor);
                if (i < preferredNeighbors.size() - 1) {
                    preferredNeighborsSB.append(", ");
                }
                i++;
            }
            String messageToLog =  "Peer " + peerID + " has the preferred neighbors " + preferredNeighborsSB.toString() + ".";
            JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);
        }
    }

    private void determineOptimisticallyUnchokedNeighbor() {
        HashSet<Integer> interestedNeighborsCopy = new HashSet<>(interestedNeighbors);

        // determine optimistically unchoked neighbor
        if (interestedNeighborsCopy.size() <= 0) { return; }

        Integer oldOptimisticallyUnchokedNeighbor = optimisticallyUnchokedNeighbor;

        // we need to consider the possibility of interested people being the exact same as preferred neighbors
        if (interestedNeighborsCopy.size() <= commonConfig.getNumberOfPreferredNeighbors() && inMemFile.isFinished(bitField, numPieces)) {
            // rip to us because we cannot do anything
            return;
        }

        boolean foundOptimisticNeighbor = false;
        int loopMax = 9000; // in case something bad happens
        while (!foundOptimisticNeighbor && loopMax > 0) {
            int randoNumber = ThreadLocalRandom.current().nextInt(0, interestedNeighborsCopy.size());
            Object[] myArr = interestedNeighborsCopy.toArray();
            Integer randomInterestedNeighbor = (Integer)myArr[randoNumber];
            if (!preferredNeighbors.contains(randomInterestedNeighbor)) {
                optimisticallyUnchokedNeighbor = randomInterestedNeighbor;
                foundOptimisticNeighbor = true;
                break;
            }
            loopMax = loopMax - 1;
        }

        // send choke/unchoke message to old/new optimistically unchoked neighbor from sever (this.server)
        if (oldOptimisticallyUnchokedNeighbor == null) {
            // send unchoke message to new neighbor
            ArrayList<Integer> unchokedClientIDs = new ArrayList<>();
            unchokedClientIDs.add(optimisticallyUnchokedNeighbor);
            Message unchokeMessage = MessageType.UNCHOKE.createMessageFromPayload(new byte[]{});
            this.server.sendMessageToListOfClients(unchokedClientIDs, unchokeMessage);
        }
        else if (!optimisticallyUnchokedNeighbor.equals(oldOptimisticallyUnchokedNeighbor)) {
            // send choke message to old optimistic neighbor
            ArrayList<Integer> chokedClientIDs = new ArrayList<>();
            chokedClientIDs.add(oldOptimisticallyUnchokedNeighbor);
            Message chokeMessage = MessageType.CHOKE.createMessageFromPayload(new byte[]{});
            this.server.sendMessageToListOfClients(chokedClientIDs, chokeMessage);

            // send unchoke message to new optimistic neighbor
            ArrayList<Integer> unchokedClientIDs = new ArrayList<>();
            unchokedClientIDs.add(optimisticallyUnchokedNeighbor);
            Message unchokeMessage = MessageType.UNCHOKE.createMessageFromPayload(new byte[]{});
            this.server.sendMessageToListOfClients(unchokedClientIDs, unchokeMessage);
        }

        // need to log the event
        String messageToLog =  "Peer " + peerID + " has the optimistically unchoked neighbor " + optimisticallyUnchokedNeighbor + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);
    }

    // ClientDelegate Methods
    @Override
    public Message onServerHandshakeReceived(Message message, int serverPeerID) {
        System.out.println("Peer " + peerID + " did receive server handshake from Peer " + serverPeerID);

        // check whether the handshake header is right and the peer ID is the expected one
        if (HandshakeMessage.class.isInstance(message)) {
            HandshakeMessage handshakeMessage = HandshakeMessage.class.cast(message);

            if (handshakeMessage.getPeerID() != serverPeerID) {
                return null;
            }
        }

        /* now that we have established a connection we need to do a few things */

        // mark that we have made a handshake with this serverPeerID
        handshakedPeers.add(serverPeerID);

        // we need to set their bitfield to nada
        serverBitfields.put(serverPeerID, new BitSet(numPieces));

        // we need to add that server to our serverDataTransferTrackingMap
        // in order to initialize the amount of data transferred from a peer to zero
        serverDataTransferTrackingMap.put(serverPeerID, 0);

        // send bitfield message to server
        return MessageType.BITFIELD.createMessageFromPayload(bitField.toByteArray());
    }

    @Override
    public Message onServerBitfieldReceived(Message message, int serverPeerID) {
        final BitSet serverBitSet = BitSet.valueOf(message.getPayloadField());
        ArrayList<Integer> missingBitIndices = inMemFile.missingPiecesFromServerBitSet(bitField, serverBitSet, numPieces);

        // we need to update our knowledge of the server's pieces
        serverBitfields.put(serverPeerID, serverBitSet);

        // Interested and Not Interested Messages have no payload
        // if has pieces I don't have send "interested" message
        if (missingBitIndices.size() > 0) {
            return MessageType.INTERESTED.createMessageFromPayload(new byte[] {});
        }
        // else sends "not interested" message
        else {
            return MessageType.NOT_INTERESTED.createMessageFromPayload(new byte[] {});
        }
    }

    @Override
    public Message onChokeReceived(Message message, int serverPeerID) {
        // need to log the event
        String messageToLog =  "Peer " + peerID + " is choked by " + serverPeerID + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

        // don't do anything else

        return null;
    }

    @Override
    public Message onUnChokeReceived(Message message, int serverPeerID) {
        // need to log the event
        String messageToLog =  "Peer " + peerID + " is unchoked by " + serverPeerID + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

        // determine missing piece that the server has
        int missingPiece = inMemFile.getMissingPieceFromServerBitSet(bitField, serverBitfields.get(serverPeerID), -1, numPieces);

        String messageWithPiece =  "and is missing piece (uc) " + missingPiece + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageWithPiece);

        // create request if anything is missing
        if (missingPiece >= 0) {
            byte[] requestPayload = ByteBuffer.allocate(4).putInt(missingPiece).array();
            return MessageType.REQUEST.createMessageFromPayload(requestPayload);
        }

        return null;
    }

    @Override
    public Message onHaveReceived(Message message, int serverPeerID) {
        // make sure we have handshaked
        if (!handshakedPeers.contains(serverPeerID)) { return null; }

        // update our knowledge of that server's bitfield
        int pieceNumber = ByteBuffer.wrap(message.getPayloadField()).getInt();
        if (pieceNumber < 0 && pieceNumber > numPieces) { return null; }

        // need to log the event
        String messageToLog =  "Peer " + peerID + " received the ‘have’ message from " + serverPeerID + " for the piece " + pieceNumber + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

        BitSet serverBitSet = serverBitfields.get(serverPeerID);
        serverBitSet.set(pieceNumber);
        serverBitfields.put(serverPeerID, serverBitSet);

        // check if we are done first and then check serverPeerID is done next so we don't need to run through all checking each time
        if (inMemFile.isFinished(bitField, numPieces) && inMemFile.isFinished(serverBitSet, numPieces)) {
            // if everyone is done then exit
            ifEveryoneIsDoneExit();
        }
        else {
            // send interested message if does not contain piece
            if (!bitField.get(pieceNumber)) {
                return MessageType.INTERESTED.createMessageFromPayload(new byte[] {});
            }

            // determine if this peer needs anything from that serverPeerID and if not send not-interested
        }

        return null;
    }

    @Override
    public Message onPieceReceived(Message message, int serverPeerID) {
        // make sure we have handshaked
        if (!handshakedPeers.contains(serverPeerID)) { return null; }

        // do nothing if we are already done
        if (inMemFile.isFinished(bitField, numPieces)) {
            return null;
        }

        // the first 4-bytes of the payload is the pieceNumber and the rest is the piece
        byte[] pieceNumberByteArray = Arrays.copyOfRange(message.getPayloadField(), 0, 4);
        int pieceNumber = ByteBuffer.wrap(pieceNumberByteArray).getInt();

        byte[] pieceByteArray = Arrays.copyOfRange(message.getPayloadField(), 4, message.getPayloadField().length);

        // need to set that piece in our InMemoryFile if not there
        if (!bitField.get(pieceNumber)) {
            inMemFile.setPiece(pieceNumber, pieceByteArray);
        }
        // update our own bitfield with the new piece
        this.bitField.set(pieceNumber);

        // need to log the event
        String messageToLog =  "Peer " + peerID + " has downloaded the piece " + pieceNumber + " from " + serverPeerID + ".";
        int numOfPiecesIHave = 0;
        for (int i = 0; i < numPieces; i++) {
            if (bitField.get(i)) {
                numOfPiecesIHave++;
            }
        }
        messageToLog = messageToLog + " Now the number of pieces it has is " + numOfPiecesIHave + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

        // need to update the serverDataTransferTrackingMap
        int currentTransferAmount = serverDataTransferTrackingMap.get(serverPeerID);
        serverDataTransferTrackingMap.put(serverPeerID, currentTransferAmount + 10);

        // grab next missing piece
        int missingPiece = inMemFile.getMissingPieceFromServerBitSet(bitField, serverBitfields.get(serverPeerID), pieceNumber, numPieces);

        // if that was the last piece then write the file
        if (inMemFile.isFinished(bitField, numPieces)) {
            // the file is done so we can write it
            writeFileToMemory();

            // send not interested to all servers
            Message notInterestedMessage = MessageType.NOT_INTERESTED.createMessageFromPayload(new byte[] {});
            this.sendMessageToAllServers(notInterestedMessage);

            // put it down here because when it was above this if statement it caused the if statement to print twice
            // notify all clients from this server that this peer now has this piece
            byte[] havePayload = ByteBuffer.allocate(4).putInt(pieceNumber).array();
            Message haveMessage = MessageType.HAVE.createMessageFromPayload(havePayload);
            this.server.sendMessageToAllClients(haveMessage);
        }
        else {
            // notify all clients from this server that this peer now has this piece
            byte[] havePayload = ByteBuffer.allocate(4).putInt(pieceNumber).array();
            Message haveMessage = MessageType.HAVE.createMessageFromPayload(havePayload);
            this.server.sendMessageToAllClients(haveMessage);

            String messageWithPiece =  "and is missing piece (p) " + missingPiece + ".";
            JitTorrentLogger.logMessageForPeerID(peerID, messageWithPiece);

            if (missingPiece >= 0) {
                // since it has a missing piece the server has then send request
                byte[] requestPayload = ByteBuffer.allocate(4).putInt(missingPiece).array();
                return MessageType.REQUEST.createMessageFromPayload(requestPayload);
            } else {
                // send not interested to all servers
                return MessageType.NOT_INTERESTED.createMessageFromPayload(new byte[] {});
            }
        }

        return null;
    }


    // Server Delegate Methods
    @Override
    public Message onClientHandshakeReceived(Message message, Consumer<Integer> setClientPeerID) {
        // Set the client peerID
        int clientPeerID = ((HandshakeMessage)message).getPeerID();
        setClientPeerID.accept(clientPeerID);

        System.out.println("Peer " + peerID + " did receive client handshake from Peer " + clientPeerID);

        // When a client reaches out to this peer, if this peer is not a client for that peer start a connection
        if (!clientConnections.containsKey(clientPeerID)){
            startClientConnection(peerInfo.get(clientPeerID));
        }

        // need to log the event here because the client one throws a lot of errors
        String messageToLog =  "Peer " + peerID + " is connected from Peer " + clientPeerID + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

        // Send a handshake back to the client
        byte[] handshakePayload = ByteBuffer.allocate(4).putInt(peerID).array();
        return MessageType.HANDSHAKE.createMessageFromPayload(handshakePayload);
    }

    @Override
    public Message onClientBitfieldReceived(Message message, int clientPeerID) {
        // send bitfield message or should skip bitfield message if doesn't have anything
        if (!bitField.isEmpty()) {
            /* have a bitfield as its payload
             * each bit in the bitfield payload represents whether the peer has the corresponding piece or not
             */
            return MessageType.BITFIELD.createMessageFromPayload(bitField.toByteArray());
        }

        return null;
    }

    @Override
    public Message onInterestedReceived(Message message, int clientPeerID) {
        // need to log the event
        String messageToLog =  "Peer " + peerID + " received the 'interested' message from " + clientPeerID + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

        // Add the client that is interested in us to interestedNeighbors
        interestedNeighbors.add(clientPeerID);

        // We do not need to do anything else right away because
        // the choking and unchoking messages are sent only on the intervals

        return null;
    }

    @Override
    public Message onNotInterestedReceived(Message message, int clientPeerID) {
        // need to log the event
        String messageToLog =  "Peer " + peerID + " received the 'not interested' message from " + clientPeerID + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);

        // if it was interested before then we should remove it
        interestedNeighbors.remove(clientPeerID);

        // We do not need to do anything else right away because
        // the choking and unchoking messages are sent only on the intervals

        return null;
    }

    @Override
    public Message onRequestReceived(Message message, int clientPeerID) {
        String messageToLog =  "Peer " + peerID + " received the 'request' message from " + clientPeerID + ".";
        JitTorrentLogger.logMessageForPeerID(peerID, messageToLog);
        // if it is one of our preferred neighbors or optimistic buddy then service the request
        // if not then ignore
        if (preferredNeighbors.contains(clientPeerID) || (optimisticallyUnchokedNeighbor != null && optimisticallyUnchokedNeighbor == clientPeerID)) {
            // get requested piece number
            int pieceNumber = ByteBuffer.wrap(message.getPayloadField()).getInt();

            String messageWithPiece = "With piece number: " + pieceNumber;
            JitTorrentLogger.logMessageForPeerID(peerID, messageWithPiece);

            // error check the bounds
            if (pieceNumber <= numPieces) {
                // get requestedPiece
                byte[] requestedPieceByteArray = inMemFile.getPiece(pieceNumber);

                // send the message
                byte[] pieceNumberByteArray = ByteBuffer.allocate(4).putInt(pieceNumber).array();

                byte[] piecePayload = new byte[pieceNumberByteArray.length + requestedPieceByteArray.length];
                System.arraycopy(pieceNumberByteArray, 0, piecePayload, 0, pieceNumberByteArray.length);
                System.arraycopy(requestedPieceByteArray, 0, piecePayload, pieceNumberByteArray.length, requestedPieceByteArray.length);
                return MessageType.PIECE.createMessageFromPayload(piecePayload);
            }
        }

        return null;
    }
}
