package app.snapshot_bitcake.coordinated_checkpointing;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KcSnapshotResult implements Serializable {
    private static final long serialVersionUID = -1443515806440079979L;

    private final int serventId;
    private final int recordedAmount;

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
}
