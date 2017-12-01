# **Welcome to the AlmightyJitTorrent!**

This is a simplified version of BitTorrent. It can be run with a common config file and a peer info config files from the terminal.
----------

## **Common.cfg**

**Description**

The first config file is the ***Common.cfg***. Everything will use this file and it needs to have the following format:

> **Example:**

> - NumberOfPreferredNeighbors 2
> - UnchokingInterval 5
> - OptimisticUnchokingInterval 15
> - FileName KitchenSink.mp3
> - FileSize 9126931
> - PieceSize 30000

----

## **PeerInfo.cfg**

**Description**

This file contains information about all of the peers that will be in the system. It should have the following format:


**File Content Organization**

[PeerID] [Hostname] [port] [hasFile]

> **Example:**

> - 1001 lin309-02.cise.ufl.edu 5090 1
> - 1002 lin309-03.cise.ufl.edu 5091 0
> - 1003 lin309-04.cise.ufl.edu 5092 0
> - 1004 lin309-05.cise.ufl.edu 5093 0
> - 1005 lin309-06.cise.ufl.edu 5094 0
> - 1006 lin309-07.cise.ufl.edu 5095 0

----

**How To Run on Remote Peers**

1. Compile ***TheJavaClassThatCreatesScripts.java*** with the command:
	* `javac TheJavaClassThatCreatesScripts.java`
2. Run ***TheJavaClassThatCreatesScripts.java*** with the command:
	* `java TheJavaClassThatCreatesScripts [username]`
3. Run ***ScriptCreateDirectories*** to make all the peer_[peerID] directories.
4. For any peers that will already have the file on startup, place the file into the appropriate peer_[peerID] directories.
5. Run ***ScriptSetupRemoteEnvironment*** to clean, build, zip, and copy files over to the remote machines.
6. Run ***ScriptRunRemotePeers*** to start the remote sessions and run the program.
7. Run ***ScriptBringThemBoysHome*** to copy the log files and downloaded files from the remote machine to the current machine.

**How To Run on Local Peers**

Follow these steps if your PeerInfo.cfg indicates that peers will be ran on localhost (on different ports).

1. Compile ***TheJavaClassThatCreatesScripts.java*** with the command:
	* `javac TheJavaClassThatCreatesScripts.java`
2. Run ***TheJavaClassThatCreatesScripts.java*** with the command:
	* `java TheJavaClassThatCreatesScripts`
3. Run ***ScriptCreateDirectories*** to make all the peer_[peerID] directories.
4. For any peers that will already have the file on startup, place the file into the appropriate peer_[peerID] directories.
5. Run ***ScriptSetupLocalEnvironments*** to clean and build.
6. Run ***ScriptRunLocalPeers*** to start and run the peers.


NOTE: If you are getting a permission error when running these scripts, you may need to run `CHMOD 700 [filename]`

Thanks for reading and have fun! :smiley:
