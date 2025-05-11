package app.snapshot_bitcake.acharya_badrinath;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.CausalBroadcastShared;
import app.snapshot_bitcake.SnapshotCollector;
import servent.message.Message;
import servent.message.snapshot.acharya_badrinath.AbResultMessage;
import servent.message.snapshot.acharya_badrinath.AbTokenMessage;
import servent.message.util.MessageUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class AbBitcakeManager implements BitcakeManager {
    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    private static final Object historyLock = new Object();
    private final Map<Integer, List<Message>> sent = new ConcurrentHashMap<>();
    private final Map<Integer, List<Message>> received = new ConcurrentHashMap<>();

    private final AtomicBoolean hasRecordedSnapshot = new AtomicBoolean(false);

    public AbBitcakeManager() {
        for (int i = 0; i < AppConfig.getServentCount(); i++) {
            sent.put(i, new CopyOnWriteArrayList<>());
            received.put(i, new CopyOnWriteArrayList<>());
        }
    }

    @Override
    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    public void recordSentTransaction(int serventId, Message message) {
        synchronized (historyLock) {
            sent.get(serventId).add(message);
        }
    }

    public void recordReceivedTransaction(int serventId, Message message) {
        synchronized (historyLock) {
            received.get(serventId).add(message);
        }
    }

    public void handleRequest(SnapshotCollector snapshotCollector) {
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

        Message tokenMessageMe = new AbTokenMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, vectorClock);
        MessageUtil.sendMessage(tokenMessageMe);

        for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
            Message tokenMessage = new AbTokenMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighborId), vectorClock);
            MessageUtil.sendMessage(tokenMessage);
        }
    }

    public void handleToken(ServentInfo collector, SnapshotCollector snapshotCollector) {
        if (hasRecordedSnapshot.getAndSet(true)) {
            AppConfig.timestampedStandardPrint("Already recorded a snapshot, ignoring token from " + collector);
            return;
        }

        int recordedAmount = getCurrentBitcakeAmount();
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

        AbSnapshotResult abSnapshotResult = new AbSnapshotResult(AppConfig.myServentInfo.getId(), recordedAmount, getSent(), getReceived());
        Message resultMessage = new AbResultMessage(AppConfig.myServentInfo, collector, vectorClock, abSnapshotResult);

        CausalBroadcastShared.commitCausalMessage(resultMessage, snapshotCollector);
        MessageUtil.sendMessage(resultMessage);
        AppConfig.timestampedStandardPrint("Sent AB_RESULT to " + collector +
            " with amount: " + recordedAmount);

        for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
            if (neighborId != collector.getId()) {
                ServentInfo neighborInfo = AppConfig.getInfoById(neighborId);
                Message tokenMessage = new AbTokenMessage(AppConfig.myServentInfo, neighborInfo, vectorClock);
                MessageUtil.sendMessage(tokenMessage);
                AppConfig.timestampedStandardPrint("Forwarded AB_TOKEN to neighbor " + neighborId);
            }
        }
    }

    public void resetSnapshotState() {
        hasRecordedSnapshot.set(false);
        // Optionally clear sent/received maps if they should be isolated per snapshot
    }

    public Map<Integer, List<Message>> getSent() {
        Map<Integer, List<Message>> toReturn = new ConcurrentHashMap<>();

        synchronized (historyLock) {
            for (Map.Entry<Integer, List<Message>> m : sent.entrySet()) {
                toReturn.put(m.getKey(), new CopyOnWriteArrayList<>(m.getValue()));
            }
        }

        return toReturn;
    }

    public Map<Integer, List<Message>> getReceived() {
        Map<Integer, List<Message>> toReturn = new ConcurrentHashMap<>();

        synchronized (historyLock) {
            for (Map.Entry<Integer, List<Message>> m : received.entrySet()) {
                toReturn.put(m.getKey(), new CopyOnWriteArrayList<>(m.getValue()));
            }
        }

        return toReturn;
    }
}
