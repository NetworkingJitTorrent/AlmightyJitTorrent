
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

public class TopDownloadPriorityQueue extends PriorityQueue<ClientDataStream> {

    private class ClientDataStreamComparator implements Comparator<ClientDataStream> {
        @Override
        public int compare(ClientDataStream x, ClientDataStream y) {
            if (x.downloadRate < y.downloadRate) {
                return -1;
            } else if (x.downloadRate > y.downloadRate) {
                return 1;
            } else {
                // we have a tie and should randomly decide
                int min = -1;
                int max = 1;
                return ThreadLocalRandom.current().nextInt(min, max + 1);
            }
        }
    }

    private ClientDataStreamComparator customComparator;

    public TopDownloadPriorityQueue() {
        customComparator = new ClientDataStreamComparator();
    }

    @Override
    public Comparator comparator() {
        return customComparator;
    }
}
