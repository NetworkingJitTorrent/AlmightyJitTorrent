import java.io.PrintWriter;
import java.util.List;
import static java.lang.System.exit;

public class TheJavaClassThatCreatesScripts {

    private static final String PEER_INFO_CONFIG = "PeerInfoRemote.cfg";
    private static final String COMMON_CONFIG = "Common.cfg";
    private static final String PROJECT_DIRECTORY = System.getProperty("user.dir");
    private static String username;

    public static void main(String[] args){
        // Validate we are given the correct number of args.
        if (args.length != 1){
            System.out.println("Invalid Arguments");
            exit(1);
        }

        List<PeerInfo> peerInfo = loadPeerInfo();
        CommonConfig commonConfig = loadCommonConfig();

        username = args[0];

        try{
            makeSetupScript(peerInfo);
            makeRunScript(peerInfo);
            bringThemBoysHome(peerInfo, commonConfig.getFileName());
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void makeSetupScript(List<PeerInfo> peerInfo) throws Exception{
        PrintWriter writer = new PrintWriter("ScriptSetupEnvironment", "UTF-8");
        writer.println("#!/bin/bash\n");

        writer.println("echo Cleaning Directory\nrm *.class");
        writer.println("echo Compiling Directory\njavac PeerProcessRemote.java");
        writer.println("echo Zipping Project\ntar -zcvf project.tgz .");

        writer.println("echo Copying Project Over");

        for (int i=0; i < peerInfo.size(); i++){
            writer.println("osascript -e 'tell application \"Terminal\" to do script \"cd " + PROJECT_DIRECTORY + "; scp project.tgz " + username +  "@" + peerInfo.get(i).getHostName() + ":~/Documents/\"'\n");
            writer.println("sleep 6s");
        }
        writer.close();
    }

    public static void makeRunScript(List<PeerInfo> peerInfo) throws Exception{
        PrintWriter writer = new PrintWriter("ScriptRunRemotePeers", "UTF-8");
        writer.println("#!/bin/bash\n");

        writer.println("echo Running Remote Peers\n");

        for (int i=0; i < peerInfo.size(); i++){
            String scriptName = "ScriptPeer" + i;
            createScriptPeer(scriptName, peerInfo.get(i), i);

            writer.println("osascript -e 'tell application \"Terminal\" to do script \"cd " + PROJECT_DIRECTORY + "; chmod 700 " + scriptName + "; ./" + scriptName + "\"'");
            writer.println("sleep 10s");
        }

        writer.close();
    }

    public static void createScriptPeer(String name, PeerInfo peerInfo, int i) throws Exception{
        PrintWriter writer = new PrintWriter(name, "UTF-8");

        writer.println("#!/bin/bash\n");

        if (i == 0) {
            writer.println("ssh " + username + "@" + peerInfo.getHostName() + " \"cd Documents/; tar -zxvf project.tgz; rm -rf logging; java PeerProcessRemote " + peerInfo.getPeerID() + "\"");
        } else {
            writer.println("ssh " + username + "@" + peerInfo.getHostName() + " \"cd Documents/; tar -zxvf project.tgz; java PeerProcessRemote " + peerInfo.getPeerID() + "\"");
        }
        writer.close();
    }

    public static void bringThemBoysHome(List<PeerInfo> peerInfo, String fileName) throws Exception{
        PrintWriter writer = new PrintWriter("ScriptBringThemBoysHome", "UTF-8");
        writer.println("#!/bin/bash\n");

        writer.println("echo Bringing Them Boys Home\n");

        for (int i=0; i < peerInfo.size(); i++){
            writer.println("osascript -e 'tell application \"Terminal\" to do script \"cd " + PROJECT_DIRECTORY + "; scp " + username +"@"+ peerInfo.get(i).getHostName()
            +":~/Documents/logging/log_peer_"+peerInfo.get(i).getPeerID()+".log "+ PROJECT_DIRECTORY +"; scp "+ username+"@"+ peerInfo.get(i).getHostName()
            +":~/Documents/peer_"+ peerInfo.get(i).getPeerID() +"/"+peerInfo.get(i).getPeerID() + fileName  +" "+ PROJECT_DIRECTORY
            + "/peer_"+peerInfo.get(i).getPeerID()+"/\"'\n");
            writer.println("sleep 12s");
        }
        writer.close();
    }

    // Load the common config file
    private static CommonConfig loadCommonConfig(){
        CommonConfig commonConfig = null;
        try{
            commonConfig = CommonConfig.createFromFile(COMMON_CONFIG);
        } catch (Exception e){
            e.printStackTrace();
            exit(1);
        }

        return commonConfig;
    }

    // Load the peer info file
    private static List<PeerInfo> loadPeerInfo(){
        List<PeerInfo> peerInfo = null;
        try {
            peerInfo = PeerInfo.createFromFile(PEER_INFO_CONFIG);
        } catch (Exception e){
            e.printStackTrace();
            exit(1);
        }

        return peerInfo;
    }
}
