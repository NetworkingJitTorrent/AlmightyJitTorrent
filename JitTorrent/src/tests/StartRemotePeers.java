package tests;

import com.company.PeerProcessRemote;
import com.company.configs.PeerInfo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static java.lang.System.exit;

public class StartRemotePeers {

    private static final String PEER_INFO_FILENAME = "PeerInfoRemote.cfg";

    // Starts up peer processes with peer id 1001 - 1006 inclusive spawning each one on a new thread.
    public static void main(String[] args) {

        List<PeerInfo> pInfo = loadPeerInfo();
        int index = 0;

        for (int i=1001; i < 1007; i++){
            String[] pArgs = new String[1];
            pArgs[0] = Integer.toString(i);
//            new Thread(() -> PeerProcessRemote.main(pArgs)).start();

            String ssh = "ssh ";
            String username = "nbmiller@";
            String command = "echo Nick";

            String exec = ssh + username + pInfo.get(index).getHostName();

//            String thing = "ssh bocinsky@" + pInfo.peerAddress + "\n cd" + path + "\n" + "mkdir test\n" + "java PeerProcess " + pInfo.peerId +"\n";
            try {
                Runtime.getRuntime().exec(exec);
            } catch (IOException e) {
                System.out.println("we got an error ssh'ing");
                e.printStackTrace();
            }

            // Delay to give the peers enough time to start. We need to make sure the peers start in the correct order
            try{
                TimeUnit.MILLISECONDS.sleep(500);
            } catch(Exception e){
                System.out.println(e);
                exit(1);
            }

            index++;
        }
    }

    // Load the peer info file
    private static List<PeerInfo> loadPeerInfo(){
        List<PeerInfo> peerInfo = null;
        try {
            peerInfo = PeerInfo.createFromFile(PEER_INFO_FILENAME);
        } catch (Exception e){
            e.printStackTrace();
            exit(1);
        }

        return peerInfo;
    }
}
