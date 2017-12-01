import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

public class InMemoryFile {

    // Attributes
    private byte[] data;
    private int pieceSize;

    public InMemoryFile(byte[] data, int pieceSize){
        this.data = data;
        this.pieceSize = pieceSize;
    }

    public byte[] getPiece(int pieceNum){

        // Determine the bounds for this piece
        int startIndex = pieceNum * pieceSize; // inclusive start
        int endIndex = startIndex + pieceSize < data.length ? startIndex + pieceSize : data.length; // not inclusive end

        // If this is not a valid piece return null.
        if (endIndex - startIndex <= 0){
            return null;
        }

        // Copy over this portion of the data
        byte[] piece = new byte[endIndex - startIndex];
        for (int i = startIndex; i < endIndex; i++){
            piece[i - startIndex] = data[i];
        }

        return piece;
    }

    // Set a piece of the file to the given data. NOTE: If the data is longer than the pieceSize (or it is the last piece and the
    // data is too long) the remainder of the data will be ignored.
    public void setPiece(int pieceNum, byte[] data){

        // Determine the bounds for this piece
        int startIndex = pieceNum * pieceSize; // inclusive start
        int endIndex = startIndex + pieceSize < this.data.length ? startIndex + pieceSize : this.data.length; // not inclusive end

        // Copy over this data the file data
        for (int i = startIndex; i < endIndex; i++){
            this.data[i] = data[i-startIndex];
        }
    }

    public static InMemoryFile createFromFile(String fileName, int pieceSize) throws Exception{
        byte[] data = Files.readAllBytes(new File(fileName).toPath());
        return new InMemoryFile(data, pieceSize);
    }

    public void writeToFile(String fileName) throws Exception{
        FileOutputStream stream = new FileOutputStream(fileName);
        stream.write(this.data);
        stream.close();
    }

    public void writeToFileWithPeerID(String fileName, int peerID) throws Exception{
        String filePath = "peer_" + peerID + "/";
        FileOutputStream stream = new FileOutputStream(filePath + fileName);
        stream.write(this.data);
        stream.close();
    }

    public boolean isFinished(BitSet clientBitSet, int numberOfPieces) {
        for (int i = 0; i < numberOfPieces; i++) {
            if (!clientBitSet.get(i)) {
                return false;
            }
        }
        return true;
    }

    public int getMissingPieceFromServerBitSet(BitSet clientBitSet, final BitSet serverBitSet, Integer currentPiece, int numberOfPieces) {
        ArrayList<Integer> missingBitIndices = missingPiecesFromServerBitSet(clientBitSet, serverBitSet, numberOfPieces);

        // remove currentPiece object because Integer
        missingBitIndices.remove(currentPiece);

        // create request if anything is missing
        if (missingBitIndices.size() > 0) {
            return missingBitIndices.get(ThreadLocalRandom.current().nextInt(0, missingBitIndices.size()));
        }

        // if bad then return -1
        return -1;
    }

    public ArrayList<Integer> missingPiecesFromServerBitSet(BitSet clientBitSet, final BitSet serverBitSet, int numberOfPieces) {
        int index = 0;
        ArrayList<Integer> missingBitIndices = new ArrayList<Integer>();
        while (index < numberOfPieces) {
            int nextClearBit = clientBitSet.nextClearBit(index);
            if (nextClearBit >= numberOfPieces) { break; } // because we have all the pieces

            if (serverBitSet.get(nextClearBit)) {
                missingBitIndices.add(nextClearBit);
            }
            index = nextClearBit + 1;
        }
        return missingBitIndices;
    }

}
