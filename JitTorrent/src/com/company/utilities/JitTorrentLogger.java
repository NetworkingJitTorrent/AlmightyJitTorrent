package com.company.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Date;

public class JitTorrentLogger {

    static private String filePath = "logging/";
    static private String filePrefix = "log_peer_";
    static private String extension = ".log";

    static public void logMessageForPeerID(int peerID, String message) {
        String fileName = filePrefix + peerID + extension;
        File logFile = new File(filePath + fileName);
        boolean firstEntry = false;
        if (!logFile.exists()) {
            firstEntry = true;
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
            }
        }

        Date date = new Date();
        message = "[" + date.toString() + "]: " + message;
        message = (!firstEntry) ? "\n" + message : message;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.append(message);
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }

    }
}
