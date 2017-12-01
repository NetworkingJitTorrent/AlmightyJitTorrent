package com.company.configs;

import com.company.utilities.FileParser;

import java.util.List;

public class PeerInfo {

    // Attributes
    private int peerID;
    private String hostName;
    private int listeningPort;
    private boolean hasFile;

    // Getters
    public int getPeerID() {
        return peerID;
    }

    public String getHostName() {
        return hostName;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public boolean getHasFile() {
        return hasFile;
    }

    // Constructor
    public PeerInfo(int peerID, String hostName, int listeningPort, boolean hasFile){
        this.peerID = peerID;
        this.hostName = hostName;
        this.listeningPort = listeningPort;
        this.hasFile = hasFile;
    }

    // Factory Method to create from config file.
    public static List<PeerInfo> createFromFile(String fileName) throws Exception{

        // Use the FileParser to read the file
        FileParser parser = new FileParser();
        return parser.parsePeerInfo(fileName);
    }

}
