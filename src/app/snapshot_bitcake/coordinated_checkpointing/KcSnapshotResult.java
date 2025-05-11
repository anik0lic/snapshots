package app.snapshot_bitcake.coordinated_checkpointing;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KcSnapshotResult implements Serializable {
    private static final long serialVersionUID = -1443515806440079979L;

    private final int serventId;
    private final int recordedAmount;
//    private final Map<String, List<Integer>> allChannelMessages;

    public KcSnapshotResult(int serventId, int recordedAmount) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
    }
    public int getServentId() {
        return serventId;
    }
    public int getRecordedAmount() {
        return recordedAmount;
    }
//    public Map<String, List<Integer>> getAllChannelMessages() {
//        return allChannelMessages;
//    }
}
