import java.io.PrintWriter;
import java.util.List;
import static java.lang.System.exit;

public class TheJavaClassThatCreatesScripts {

    private static final String PROJECT_DIRECTORY = System.getProperty("user.dir");
    private static final String COMMON_CONFIG = "Common.cfg";
    private static final String PEER_INFO = "PeerInfo.cfg";
    private static String username;

    public static void main(String[] args){

        CommonConfig commonConfig = loadCommonConfig(COMMON_CONFIG);
        List<PeerInfo> peerInfo = loadPeerInfo(PEER_INFO);

        // Validate we are given the correct number of args.
        if (args.length == 1) {

          // Create Scripts to run remotely
          username = args[0];

          try{
              makeDirectoriesScript(peerInfo);
              makeSetupRemoteScript(peerInfo);
              makeRunRemoteScript(peerInfo);
              bringThemBoysHome(peerInfo, commonConfig.getFileName());
          } catch(Exception e){
              e.printStackTrace();
          }
        } else {
          // Create scripts to run locally
          try{
            makeDirectoriesScript(peerInfo);
            makeSetupLocalScript(peerInfo);
            makeRunLocalScript(peerInfo);
          } catch (Exception e){
            e.printStackTrace();
          }
        }
    }

    public static void makeDirectoriesScript(List<PeerInfo> peerInfo) throws Exception{
      PrintWriter writer = new PrintWriter("ScriptCreateDirectories", "UTF-8");
      writer.println("#!/bin/bash\n");

      writer.println("echo Creating Peer Directories\n");

      for (int i=0; i < peerInfo.size(); i++){
          writer.println("mkdir peer_" + peerInfo.get(i).getPeerID());
      }

      writer.close();
    }

    public static void makeSetupLocalScript(List<PeerInfo> peerInfo) throws Exception{
        PrintWriter writer = new PrintWriter("ScriptSetupLocalEnvironments", "UTF-8");
        writer.println("#!/bin/bash\n");

        writer.println("echo Cleaning Directory\nrm *.class");
        writer.println("echo Compiling Directory\njavac PeerProcess.java");
        writer.close();
    }

    public static void makeSetupRemoteScript(List<PeerInfo> peerInfo) throws Exception{
        PrintWriter writer = new PrintWriter("ScriptSetupRemoteEnvironments", "UTF-8");
        writer.println("#!/bin/bash\n");

        writer.println("echo Cleaning Directory\nrm *.class");
        writer.println("echo Compiling Directory\njavac PeerProcess.java");
        writer.println("echo Zipping Project\ntar -zcvf project.tgz .");

        writer.println("echo Copying Project Over");

        for (int i=0; i < peerInfo.size(); i++){
            writer.println("osascript -e 'tell application \"Terminal\" to do script \"cd " + PROJECT_DIRECTORY + "; scp project.tgz " + username +  "@" + peerInfo.get(i).getHostName() + ":~/Documents/\"'\n");
            writer.println("sleep 6s");
        }
        writer.close();
    }
    public static void makeRunLocalScript(List<PeerInfo> peerInfo) throws Exception{
        PrintWriter writer = new PrintWriter("ScriptRunLocalPeers", "UTF-8");
        writer.println("#!/bin/bash\n");

        writer.println("echo Running Peers\n");

        for (int i=0; i < peerInfo.size(); i++){
            String scriptName = "ScriptLocalPeer" + i;
            createLocalScriptPeer(scriptName, peerInfo.get(i), i);

            writer.println("osascript -e 'tell application \"Terminal\" to do script \"cd " + PROJECT_DIRECTORY + "; chmod 700 " + scriptName + "; ./" + scriptName + "\"'");
            writer.println("sleep 1s");
        }

        writer.close();
    }

    public static void makeRunRemoteScript(List<PeerInfo> peerInfo) throws Exception{
        PrintWriter writer = new PrintWriter("ScriptRunRemotePeers", "UTF-8");
        writer.println("#!/bin/bash\n");

        writer.println("echo Running Peers\n");

        for (int i=0; i < peerInfo.size(); i++){
            String scriptName = "ScriptRemotePeer" + i;
            createRemoteScriptPeer(scriptName, peerInfo.get(i), i);

            writer.println("osascript -e 'tell application \"Terminal\" to do script \"cd " + PROJECT_DIRECTORY + "; chmod 700 " + scriptName + "; ./" + scriptName + "\"'");
            writer.println("sleep 10s");
        }

        writer.close();
    }

    public static void createRemoteScriptPeer(String name, PeerInfo peerInfo, int i) throws Exception{
        PrintWriter writer = new PrintWriter(name, "UTF-8");

        writer.println("#!/bin/bash\n");

        if (i == 0) {
            writer.println("ssh " + username + "@" + peerInfo.getHostName() + " \"cd Documents/; tar -zxvf project.tgz; rm -rf logging; java PeerProcess " + peerInfo.getPeerID() + "\"");
        } else {
            writer.println("ssh " + username + "@" + peerInfo.getHostName() + " \"cd Documents/; tar -zxvf project.tgz; java PeerProcess " + peerInfo.getPeerID() + "\"");
        }
        writer.close();
    }

    public static void createLocalScriptPeer(String name, PeerInfo peerInfo, int i) throws Exception{
        PrintWriter writer = new PrintWriter(name, "UTF-8");

        writer.println("#!/bin/bash\n");

        if (i == 0) {
            writer.println("cd " + PROJECT_DIRECTORY);
            writer.println("rm -rf logging");
            writer.println("java PeerProcess " + peerInfo.getPeerID());
        } else {
            writer.println("cd " + PROJECT_DIRECTORY);
            writer.println("java PeerProcess " + peerInfo.getPeerID());
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
    private static CommonConfig loadCommonConfig(String filename){
        CommonConfig commonConfig = null;
        try{
            commonConfig = CommonConfig.createFromFile(filename);
        } catch (Exception e){
            e.printStackTrace();
            exit(1);
        }

        return commonConfig;
    }

    // Load the peer info file
    private static List<PeerInfo> loadPeerInfo(String filename){
        List<PeerInfo> peerInfo = null;
        try {
            peerInfo = PeerInfo.createFromFile(filename);
        } catch (Exception e){
            e.printStackTrace();
            exit(1);
        }

        return peerInfo;
    }
}
