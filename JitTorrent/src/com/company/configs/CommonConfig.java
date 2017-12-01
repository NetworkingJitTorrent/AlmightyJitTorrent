package com.company.configs;

import com.company.utilities.FileParser;

import java.util.HashMap;
import java.util.Map;

// This is the object representation of the Common.cfg file
public class CommonConfig {

    // Attributes
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    // Getters
    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    // Constructor
    public CommonConfig(int numberOfPreferredNeighbors,
            int unchokingInterval,
            int optimisticUnchokingInterval,
            String fileName,
            int fileSize,
            int pieceSize){
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
    }

    // Factory Method to create from config file.
    public static CommonConfig createFromFile(String fileName) throws Exception{

        // Use the FileParser to get the key value pairs from the file.
        FileParser parser = new FileParser();
        HashMap<String, String> map = parser.parseHashMap(fileName);
        int foundAttributes = 0;

        // Traverse the keyset and set variables for each attribute.
        int numberOfPreferredNeighbors = -1;
        int unchokingInterval = -1;
        int optimisticUnchokingInterval = -1;
        String interestedFileName = "";
        int fileSize = -1;
        int pieceSize = -1;


        for (Map.Entry<String, String> entry : map.entrySet()){
            switch(entry.getKey()){
                case "NumberOfPreferredNeighbors":
                    numberOfPreferredNeighbors = Integer.parseInt(entry.getValue());
                    foundAttributes++;
                    break;
                case "UnchokingInterval":
                    unchokingInterval = Integer.parseInt(entry.getValue());
                    foundAttributes++;
                    break;
                case "OptimisticUnchokingInterval":
                    optimisticUnchokingInterval = Integer.parseInt(entry.getValue());
                    foundAttributes++;
                    break;
                case "FileName":
                    interestedFileName = entry.getValue();
                    foundAttributes++;
                    break;
                case "FileSize":
                    fileSize = Integer.parseInt(entry.getValue());
                    foundAttributes++;
                    break;
                case "PieceSize":
                    pieceSize = Integer.parseInt(entry.getValue());
                    foundAttributes++;
                    break;
            }
        }

        if (foundAttributes != 6){
            throw new Exception("Invalid config file not all attributes found.");
        }

        return new CommonConfig(numberOfPreferredNeighbors,
                unchokingInterval,
                optimisticUnchokingInterval,
                interestedFileName,
                fileSize,
                pieceSize);
    }
}
