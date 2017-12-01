# **Welcome to the AlmightyJitTorrent!**

This is a simplified version of BitTorrent. It can be run with a common config file and from two different peer info config files and has both an Intellij Project and Command Line Project.

----------

## **Common.cfg**

**Description**
The first config file is the ***Common.cfg***. Everything will use this file and it needs to have all the information below (excluding the values).

> **Example:**

> NumberOfPreferredNeighbors 2
> UnchokingInterval 5
> OptimisticUnchokingInterval 15
> FileName KitchenSink.mp3
> FileSize 9126931
> PieceSize 30000

----

## **PeerInfo.cfg and PeerInfoRemote.cfg**

__**Description**__
These are used by two different files to create the peers. If you are running on localhosts then you need to use ***PeerInfo.cfg*** and compile/run ***StartTestEnv.java***. If you are running remote peers then you will need to use ***PeerInfoRemote.cfg***, compile/run ***TheJavaClassThatCreatesScripts.java*** (where the command line argument is your username for the remote machines), and then run three scripts (***ScriptSetupEnvironment***, ***ScriptRunRemotePeers***, and ***ScriptBringThemBoysHome***).

**File Content Organization**
The first attribute is the PeerID.
The second attribute is the host name.
The third attribute is the port number.
The fourth attribute is a 1 if the peer contains the file or a 0 if not.

### **PeerInfo.cfg**
**Description**
The first type of peer info config file is the ***PeerInfo.cfg***. This is the config used for a localhost, or a  non-remote session. You can test it by compiling and running ***StartTestEnv.java***.

> **Example:**

> - 1001 localhost 6008 1
> - 1002 localhost 6009 0
> - 1003 localhost 6010 0
> - 1004 localhost 6011 0
> - 1005 localhost 6012 0
> - 1006 localhost 6013 0

### **PeerInfoRemote.cfg**
**Description**
The second type of peer info config file is the ***PeerInfoRemote.cfg***. This is the config used for remote sessions.

**How To Run**

1. Compile ***TheJavaClassThatCreatesScripts.java*** with the command:
	* javac TheJavaClassThatCreatesScripts.java
2. Run ***TheJavaClassThatCreatesScripts.java*** with the command:
	* java TheJavaClassThatCreatesScripts [insert your username]
3. Run ***ScriptSetupEnvironment*** to clean, build, zip, and copy files over to the remote machines.
4. Run ***ScriptRunRemotePeers*** to start the remote sessions and run the program.
5. Run ***ScriptBringThemBoysHome*** to copy the log files and downloaded files from the remote machine to the current machine.

> **Example:**

> 1001 lin309-02.cise.ufl.edu 5090 1
> 1002 lin309-03.cise.ufl.edu 5091 0
> 1003 lin309-04.cise.ufl.edu 5092 0
> 1004 lin309-05.cise.ufl.edu 5093 0
> 1005 lin309-06.cise.ufl.edu 5094 0
> 1006 lin309-07.cise.ufl.edu 5095 0

----

Thanks for reading and have fun! :smiley:
