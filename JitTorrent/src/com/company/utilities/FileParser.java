package com.company.utilities;

import com.company.configs.PeerInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileParser {

    // This method expects to receive a file where each line is key value pair.
    // It returns the hashmap representation of that file.
    public HashMap<String, String>  parseHashMap(String fileName) throws Exception{

        HashMap<String, String> result = new HashMap<>();
        BufferedReader input = new BufferedReader(new FileReader(fileName));

        String currentLine;
        while((currentLine = input.readLine()) != null){

            // Extract each key value pair.
            String[] splitLine = currentLine.split("\\s+");

            // Verify that two values have been read.
            if (splitLine.length != 2){
                throw new Exception("Invalid file format. Must contain only key value pairs.");
            }

            result.put(splitLine[0], splitLine[1]);
        }

        return result;
    }

    // This method returns a list of PeerInfo objects given a file that matches
    // the expected PeerInfo config format.
    public List<PeerInfo> parsePeerInfo(String fileName) throws Exception{

        List<PeerInfo> result = new ArrayList<>();
        BufferedReader input = new BufferedReader(new FileReader(fileName));

        String currentLine;
        while((currentLine = input.readLine()) != null){

            // Extract each value from the line
            String[] splitLine = currentLine.split("\\s+");

            // Verify that all four values have been found.
            if (splitLine.length != 4){
                throw new Exception("Invalid attributes for peer info.");
            }

            result.add(new PeerInfo(Integer.parseInt(splitLine[0]),
                    splitLine[1],
                    Integer.parseInt(splitLine[2]),
                    splitLine[3].equals("1") ? true : false));
        }

        return result;
    }
}
