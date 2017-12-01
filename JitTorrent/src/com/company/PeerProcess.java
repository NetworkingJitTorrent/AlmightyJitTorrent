package com.company;

import com.company.configs.CommonConfig;
import com.company.configs.PeerInfo;

import java.util.List;

import static java.lang.System.exit;

public class PeerProcess {

    private static final String COMMON_CONFIG_FILENAME = "Common.cfg";
    private static final String PEER_INFO_FILENAME = "PeerInfo.cfg";


    public static void main(String[] args) {

        // Validate we are given the correct number of args.
        if (args.length != 1){
            System.out.println("Invalid Arguments");
            exit(1);
        }

        // Read the peerID from the args
        int peerID = Integer.parseInt(args[0]);

        // Load the common config
        CommonConfig commonConfig = loadCommonConfig();

        // Create the peer
        Peer peer = new Peer(peerID, commonConfig);

        // Load the peer info
        List<PeerInfo> peerInfo = loadPeerInfo();

        // Start the peer
        try{
            peer.start(peerInfo);
        } catch (Exception e){
            e.printStackTrace();
            exit(1);
        }
    }

    // Load the common config file
    private static CommonConfig loadCommonConfig(){
        CommonConfig commonConfig = null;
        try{
            commonConfig = CommonConfig.createFromFile(COMMON_CONFIG_FILENAME);
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
            peerInfo = PeerInfo.createFromFile(PEER_INFO_FILENAME);
        } catch (Exception e){
            e.printStackTrace();
            exit(1);
        }

        return peerInfo;
    }
}
