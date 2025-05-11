package app.snapshot_bitcake.alagar_venkatesan;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.message.CausalBroadcastShared;
import servent.message.Message;
import servent.message.snapshot.alagar_venkatesan.AvDoneMessage;
import servent.message.snapshot.alagar_venkatesan.AvTerminateMessage;
import servent.message.snapshot.alagar_venkatesan.AvTokenMessage;
import servent.message.util.MessageUtil;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AvBitcakeManager implements BitcakeManager {
    private final AtomicInteger currentAmount = new AtomicInteger(1000);
    private final AtomicBoolean avRecording = new AtomicBoolean(false);
    private final AtomicBoolean avTerminated = new AtomicBoolean(false);
    private final AtomicInteger avRecordedAmount = new AtomicInteger(0);
    private final Set<Integer> avDoneReceived = ConcurrentHashMap.newKeySet();
    private final Map<Integer, List<Message>> avChannelStates = new ConcurrentHashMap<>();


    public void takeSomeBitcakes(int amount) {
        currentAmount.addAndGet(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.addAndGet(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    public void handleRequest(SnapshotCollector snapshotCollector) {
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

        Message tokenMessageMe = new AvTokenMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, vectorClock, AppConfig.myServentInfo);
        MessageUtil.sendMessage(tokenMessageMe);

        for (int i = 0; i < AppConfig.getServentCount(); i++) {
            if (i == AppConfig.myServentInfo.getId()) continue;

            Message tokenMessage = new AvTokenMessage(AppConfig.myServentInfo, AppConfig.getInfoById(i), vectorClock, AppConfig.myServentInfo);
            MessageUtil.sendMessage(tokenMessage);
        }
    }

    public void handleToken(Message message, SnapshotCollector snapshotCollector) {
        if (avRecording.get()) return;
        AvTokenMessage tokenMessage = (AvTokenMessage) message;
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

        avRecording.set(true);
        avTerminated.set(false);
        avDoneReceived.clear();
        avChannelStates.clear();

        for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
            avChannelStates.put(neighborId, new ArrayList<>());
        }

        int amount = getCurrentBitcakeAmount();
        avRecordedAmount.set(amount);

        AppConfig.timestampedStandardPrint("Received AV_TOKEN, recorded: " + amount + " bitcakes");

        Message done = new AvDoneMessage(AppConfig.myServentInfo, tokenMessage.getCollector(), vectorClock);
        CausalBroadcastShared.commitCausalMessage(done, snapshotCollector);
        MessageUtil.sendMessage(done);
    }

    public void handleDone(Message message, SnapshotCollector snapshotCollector) {
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

        int senderId = message.getOriginalSenderInfo().getId();
        avDoneReceived.add(senderId);

        AppConfig.timestampedStandardPrint("Received AV_DONE from " + senderId);
        AvSnapshotResult snapshotResult = new AvSnapshotResult(
                getCurrentBitcakeAmount()
        );
        snapshotCollector.addAvSnapshotInfo(senderId, snapshotResult);

        if (avDoneReceived.size() == AppConfig.getServentCount() - 1) {
            AppConfig.timestampedStandardPrint("All AV_DONE received, sending TERMINATE to neighbors");

            for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
                Message terminateMessage = new AvTerminateMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighborId), vectorClock);
                CausalBroadcastShared.commitCausalMessage(terminateMessage, snapshotCollector);
                MessageUtil.sendMessage(terminateMessage);
            }

            avTerminated.set(true);
            resetAvSnapshot();
        }
    }

    public void handleTerminate(Message message, SnapshotCollector snapshotCollector) {
        if (!avRecording.get()) return;

        avTerminated.set(true);
        AppConfig.timestampedStandardPrint("Received AV_TERMINATE from " + message.getOriginalSenderInfo().getId());
        resetAvSnapshot();
    }

    public void recordAvChannelMessage(Message message) {
        if (avRecording.get() && !avTerminated.get()) {
            int senderId = message.getOriginalSenderInfo().getId();
            avChannelStates.computeIfAbsent(senderId, k -> new ArrayList<>()).add(message);

        }
    }

    private void resetAvSnapshot() {
        avRecording.set(false);
        avTerminated.set(false);
        avChannelStates.clear();
        avDoneReceived.clear();
    }


}
