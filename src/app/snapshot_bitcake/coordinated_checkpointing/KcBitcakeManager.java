package app.snapshot_bitcake.coordinated_checkpointing;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.message.Message;
import servent.message.snapshot.coordinated_checkpointing.KcAckMessage;
import servent.message.snapshot.coordinated_checkpointing.KcRequestMessage;
import servent.message.util.MessageUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class KcBitcakeManager implements BitcakeManager {
    private final AtomicInteger currentAmount = new AtomicInteger(1000);
    private final AtomicBoolean blocked = new AtomicBoolean(false);
    private final AtomicInteger snapshotAmount = new AtomicInteger(0);

    @Override
    public void takeSomeBitcakes(int amount) {
        if (!blocked.get()) {
            currentAmount.getAndAdd(-amount);
        }
    }

    @Override
    public void addSomeBitcakes(int amount) {
        if (!blocked.get()) {
            currentAmount.getAndAdd(amount);
        }
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    public void handleRequest (Message message, SnapshotCollector snapshotCollector){
        int initiatorId = message.getOriginalSenderInfo().getId();

        if (blocked.get()) {
            AppConfig.timestampedErrorPrint("Snapshot is in progress, cannot handle request");
            return;
        }

        AppConfig.timestampedStandardPrint("Handling snapshot request from " + initiatorId);
        blocked.set(true);
        int recordedAmount = currentAmount.get();
        snapshotAmount.set(recordedAmount);

        KcSnapshotResult result = new KcSnapshotResult(AppConfig.myServentInfo.getId(), recordedAmount);

        if (initiatorId == AppConfig.myServentInfo.getId()) {
            snapshotCollector.addKcSnapshotInfo(initiatorId, result);
        } else {
            KcAckMessage ackMessage = new KcAckMessage(AppConfig.myServentInfo, AppConfig.getInfoById(initiatorId), String.valueOf(recordedAmount));
            MessageUtil.sendMessage(ackMessage);
            AppConfig.timestampedStandardPrint("Send snapshot ack to " + initiatorId);
        }

        for (Integer neighborId : AppConfig.myServentInfo.getNeighbors()) {
            if (neighborId != initiatorId) {
                KcRequestMessage requestMessage = new KcRequestMessage(message.getOriginalSenderInfo(), AppConfig.getInfoById(neighborId));
                MessageUtil.sendMessage(requestMessage);
                AppConfig.timestampedStandardPrint("Forward snapshot request to " + neighborId);
            }
        }

    }

    public void handleResume(){
        blocked.set(false);
        snapshotAmount.set(0);
    }
}
