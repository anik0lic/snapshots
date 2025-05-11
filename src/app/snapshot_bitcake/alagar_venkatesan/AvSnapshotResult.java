package app.snapshot_bitcake.alagar_venkatesan;

import java.io.Serializable;

public class AvSnapshotResult implements Serializable {
    private static final long serialVersionUID = -1093425806440079979L;

    private final int recordedAmount;;

    public AvSnapshotResult(int recordedAmount) {
        this.recordedAmount = recordedAmount;
    }

    public int getRecordedAmount() { return recordedAmount; }
}
