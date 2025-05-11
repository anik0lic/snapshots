package app.snapshot_bitcake.acharya_badrinath;

import servent.message.Message;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AbSnapshotResult implements Serializable {
    private static final long serialVersionUID = -1093425806440079979L;

    private final int serventId;
    private final int recordedAmount;
    private final Map<Integer, List<Message>> sent;
    private final Map<Integer, List<Message>> received;


    public AbSnapshotResult(int serventId, int recordedAmount, Map<Integer, List<Message>> sent, Map<Integer, List<Message>> received) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
        this.sent = new ConcurrentHashMap<>(sent);
        this.received = new ConcurrentHashMap<>(received);
    }

    public int getServentId() {
        return serventId;
    }

    public int getRecordedAmount() { return recordedAmount; }

    public Map<Integer, List<Message>> getSent() { return sent; }

    public Map<Integer, List<Message>> getReceived() { return received; }

}
