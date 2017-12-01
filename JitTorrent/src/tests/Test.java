package tests;

import com.company.services.Server;
import com.company.utilities.InMemoryFile;

import java.io.File;
import java.io.RandomAccessFile;

import static java.lang.System.exit;

// NOTE: These aren't actually tests... Just a class I've been using to "test" code as I write it.
public class Test {

    private static final String HOST_NAME = "localhost";
    private static final int PORT = 8000;
    private static final int PIECE_SIZE = 5;
    private static final int FILE_SIZE = 13;


    public static void main(String[] args) {

//        verifyServer();
//        createFile();
        inMemoryFileTests();
    }

    private static void inMemoryFileTests(){

        final String FILE_NAME = "./TheFile.dat";
        final String NEW_FILE_NAME = "./NewTheFile.dat";

        createFile(FILE_NAME);

        InMemoryFile file = null;
        try{
           file = InMemoryFile.createFromFile(FILE_NAME, PIECE_SIZE);
        } catch(Exception e){
            System.out.println(e);
            exit(1);
        }

        printPiece(file.getPiece(0));
        printPiece(file.getPiece(1));
        printPiece(file.getPiece(2));

        // Test Writing all 1s to the beginning
        byte[] data = new byte[PIECE_SIZE];
        for (int i = 0; i < PIECE_SIZE; i++){
            data[i] = 1;
        }
        file.setPiece(0, data);

        printPiece(file.getPiece(0));
        printPiece(file.getPiece(1));
        printPiece(file.getPiece(2));

        // TEST Writing 2s to the middle
        byte[] data2 = new byte[PIECE_SIZE];
        for (int i = 0; i < PIECE_SIZE; i++){
            data2[i] = 2;
        }
        file.setPiece(1, data2);

        printPiece(file.getPiece(0));
        printPiece(file.getPiece(1));
        printPiece(file.getPiece(2));

        // TEST Writing 3s to end with data that is too large
        byte[] data3 = new byte[PIECE_SIZE];
        for (int i = 0; i < PIECE_SIZE; i++){
            data3[i] = 3;
        }
        file.setPiece(2, data3);

        printPiece(file.getPiece(0));
        printPiece(file.getPiece(1));
        printPiece(file.getPiece(2));

        // TEST Writing 4s to end with data that is the right size
        byte[] data4 = new byte[PIECE_SIZE * 3 - FILE_SIZE + 1];
        for (int i = 0; i < data4.length; i++){
            data4[i] = 4;
        }
        file.setPiece(2, data4);

        printPiece(file.getPiece(0));
        printPiece(file.getPiece(1));
        printPiece(file.getPiece(2));

        // Save the file
        try{
            file.writeToFile(NEW_FILE_NAME);
        } catch( Exception e){
            System.out.println(e);
            exit(1);
        }

        // Read the saved file and make sure it looks right.
        InMemoryFile newFile = null;
        try{
            newFile = InMemoryFile.createFromFile(NEW_FILE_NAME, PIECE_SIZE);
        } catch(Exception e){
            System.out.println(e);
            exit(1);
        }

        printPiece(newFile.getPiece(0));
        printPiece(newFile.getPiece(1));
        printPiece(newFile.getPiece(2));

        // CLean up the files
        new File(FILE_NAME).delete();
        new File(NEW_FILE_NAME).delete();

    }

    private static void printPiece(byte[] data){
        for (int i=0; i < data.length; i++){
            System.out.print(data[i]);
        }
        System.out.println("");
    }

    private static void createFile(String fileName){
        try{
            new RandomAccessFile(fileName, "rw").setLength(FILE_SIZE);
            System.out.println("File Created");
        } catch(Exception e){
            System.out.println(e);
            exit(1);
        }
    }

    private static void verifyServer(){
//        Server server = new Server();
//
//        (new Thread(new ServerStarter(server, PORT))).start();
//
//        Client client = new Client();
//        client.startConnection(HOST_NAME, PORT);
    }

    private static class ServerStarter implements Runnable{

        private Server server;
        private int port;

        ServerStarter(Server server, int port){
            this.server = server;
            this.port = port;
        }

        @Override
        public void run() {
            try{
                server.start(port);
            } catch(Exception e){
                System.out.println(e);
                exit(1);
            }
        }
    }
}
