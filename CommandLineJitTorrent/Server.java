
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private ServerDelegate delegate;
    private ServerSocket listener;
    private List<HandlerThread> handlerThreads;

    public Server(ServerDelegate delegate){

        this.delegate = delegate;
        this.handlerThreads = new ArrayList<>();
    }

    public void start(int port) throws Exception{

        listener = new ServerSocket(port);

        while (true) {
            // Start a new thread to handle an incoming connection
            HandlerThread t = new HandlerThread(listener.accept(), delegate);
            handlerThreads.add(t);
            t.start();
        }
    }

    public void closeConnection() throws Exception{
        listener.close();
    }

    // Yo Nick call this method from Peer when you need to send have message to everyone.
    public void sendMessageToAllClients(Message m){
        handlerThreads.forEach((thread) -> {
            if(thread.isAlive()){ // TODO: Should probably remove threads from the list once the thread goes inactive but too lazy right now and it won't break anything.
                thread.addOutboundMessage(m); // TODO: Also we haven't really implemented closing connections as part of the protocol yet so maybe do this with that stuff.
            }
        });
    }

    // send message to list of clients
    public void sendMessageToListOfClients(ArrayList<Integer> requestedClientIDs, Message m) {
        handlerThreads.forEach((thread) -> {
            if(thread.isAlive() && requestedClientIDs.contains(thread.clientPeerID)){
                thread.addOutboundMessage(m);
            }
        });
    }

    // Each of these threads will be created to handle each client request.
    private static class HandlerThread extends Thread {
        private int clientPeerID;
        private ServerDelegate delegate;
        private ConnectionHelper connectionHelper;

        public HandlerThread(Socket socket, ServerDelegate delegate) {
            this.connectionHelper = new ConnectionHelper(socket);
            this.delegate = delegate;
            this.clientPeerID = -1;
        }

        public void run() {
            try{
                while (true)
                {
                    // Listen on the input port
                    connectionHelper.listenForMessages();

                    // Receive inbound messsages
                    connectionHelper.receiveInboundMessages(this::notifyDelegate);

                    // Send outbound messages
                    connectionHelper.sendOutboundMessages();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally{
                try {
                    connectionHelper.closeConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Adds a message to the standard outbound message queue
        public void addOutboundMessage(Message m){
            connectionHelper.addOutboundMessage(m);
        }

        private void notifyDelegate(Message message){

            Message m;
            switch(message.getType()){
                case HANDSHAKE:
                    m =  delegate.onClientHandshakeReceived(message, this::setClientPeerID);
                    break;
                case BITFIELD:
                    m = delegate.onClientBitfieldReceived(message, clientPeerID);
                    break;
                case INTERESTED:
                    m = delegate.onInterestedReceived(message, clientPeerID);
                    break;
                case NOT_INTERESTED:
                    m = delegate.onNotInterestedReceived(message, clientPeerID);
                    break;
                case REQUEST:
                    m = delegate.onRequestReceived(message, clientPeerID);
                    break;
                default:
                    m = null;
            }

            if (m != null){
                addOutboundMessage(m);
            }
        }

        private void setClientPeerID(int clientPeerID){

            this.clientPeerID = clientPeerID;
        }
    }
}
