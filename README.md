# **Welcome to the AlmightyJitTorrent!**

This is a simplified version of BitTorrent. It can be run with a common config file and a peer info config files and has both an Intellij Project and Command Line Project.

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

The first attribute is the PeerID.
The second attribute is the host name.
The third attribute is the port number.
The fourth attribute is a 1 if the peer contains the file or a 0 if not.

> **Example:**

> - 1001 lin309-02.cise.ufl.edu 5090 1
> - 1002 lin309-03.cise.ufl.edu 5091 0
> - 1003 lin309-04.cise.ufl.edu 5092 0
> - 1004 lin309-05.cise.ufl.edu 5093 0
> - 1005 lin309-06.cise.ufl.edu 5094 0
> - 1006 lin309-07.cise.ufl.edu 5095 0

----

**How To Run**

1. Compile ***TheJavaClassThatCreatesScripts.java*** with the command:
	* `javac TheJavaClassThatCreatesScripts.java`
2. Run ***TheJavaClassThatCreatesScripts.java*** with the command:
	* `java TheJavaClassThatCreatesScripts [username] [common config filename] [peer info filename]`
3. Run ***ScriptMakeDirectories*** to make all the peer_[peerID] directories.
4. For any peers that will already have the file on startup, place the file into the appropriate peer_[peerID] directories.
5. Run ***ScriptSetupEnvironment*** to clean, build, zip, and copy files over to the remote machines.
6. Run ***ScriptRunRemotePeers*** to start the remote sessions and run the program.
7. Run ***ScriptBringThemBoysHome*** to copy the log files and downloaded files from the remote machine to the current machine.

NOTE: If you are getting a permission error when running these scripts, you may need to run `CHMOD 700 [filename]`

Thanks for reading and have fun! :smiley:
