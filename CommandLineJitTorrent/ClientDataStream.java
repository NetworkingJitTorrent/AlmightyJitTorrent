
import java.util.concurrent.ThreadLocalRandom;

public class ClientDataStream implements Comparable<ClientDataStream> {
    public int id;
    public int downloadAmount;
    public float downloadRate;

    public ClientDataStream(int id, int downloadAmount, int timeInterval) {
        this.id = id;
        this.downloadAmount = downloadAmount;
        calculateDownloadRate(timeInterval);
    }

    private void calculateDownloadRate(int timeInterval) {
        this.downloadRate = (float)(downloadAmount / timeInterval);
    }

    @Override
    public int compareTo(ClientDataStream o) {
        if (o.downloadRate < this.downloadRate) {
            return -1;
        } else if (o.downloadRate > this.downloadRate) {
            return 1;
        } else {
            // we have a tie and should randomly decide
            int min = -1;
            int max = 1;
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
    }
}
