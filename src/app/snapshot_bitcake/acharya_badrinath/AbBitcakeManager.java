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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class AbBitcakeManager implements BitcakeManager {
    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    private static final Object historyLock = new Object();
    private final Map<Integer, List<Message>> sent = new ConcurrentHashMap<>();
    private final Map<Integer, List<Message>> received = new ConcurrentHashMap<>();

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

    public void startSnapshot(SnapshotCollector snapshotCollector) {
            Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

            Message abTokenMessage = new AbTokenMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, vectorClock);

            CausalBroadcastShared.addPendingMessage(abTokenMessage);
            CausalBroadcastShared.checkPendingMessages(snapshotCollector);

            for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
                Message abTokenMessageNeighbor = new AbTokenMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighborId), vectorClock);
//                Message abTokenMessageNeighbor = abTokenMessage.changeReceiver(neighborId);

                CausalBroadcastShared.commitCausalMessage(abTokenMessageNeighbor, snapshotCollector);
                MessageUtil.sendMessage(abTokenMessageNeighbor.changeReceiver(neighborId).makeMeASender());

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

//            CausalBroadcastShared.commitCausalMessage(abTokenMessage.changeReceiver(AppConfig.myServentInfo.getId()), snapshotCollector);
//
//            AbSnapshotResult result = new AbSnapshotResult(AppConfig.myServentInfo.getId(), getCurrentBitcakeAmount(), sent, received);
//            return result;
    }

    public void handleToken(ServentInfo collector, SnapshotCollector snapshotCollector) {
            int recordedAmount = getCurrentBitcakeAmount();
            Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

            AbSnapshotResult abSnapshotResult = new AbSnapshotResult(AppConfig.myServentInfo.getId(), recordedAmount, getSent(), getReceived());

            Message resultMessage = new AbResultMessage(AppConfig.myServentInfo, collector, vectorClock, abSnapshotResult);
            CausalBroadcastShared.commitCausalMessage(resultMessage, snapshotCollector);
            MessageUtil.sendMessage(resultMessage);
            AppConfig.timestampedStandardPrint("Sent AB tell response to " + collector +
                " with amount: " + currentAmount);
//            for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
//                resultMessage = resultMessage.changeReceiver(neighborId);
//
//                MessageUtil.sendMessage(resultMessage);
//            }

//            Message resultCommitMessage = new AbResultMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, vectorClock, abSnapshotResult);
//            CausalBroadcastShared.commitCausalMessage(resultCommitMessage, snapshotCollector);

//            if (collector.getId() == AppConfig.myServentInfo.getId()) {
//                Message abResultMessage = new AbResultMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, vectorClock, abSnapshotResult);
//
//                CausalBroadcastShared.addPendingMessage(abResultMessage);
//                CausalBroadcastShared.checkPendingMessages(snapshotCollector);
//            }
//            else {
//                for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
//                    Message abResultMessageNeighbor = new AbResultMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighborId), vectorClock, abSnapshotResult);
//
//                    CausalBroadcastShared.commitCausalMessage(abResultMessageNeighbor, snapshotCollector);
//                    MessageUtil.sendMessage(abResultMessageNeighbor.changeReceiver(neighborId).makeMeASender());
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
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
