package servent.handler.snapshot.coordinated_checkpointing;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.coordinated_checkpointing.KcSnapshotResult;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class KcAckHandler implements MessageHandler {
    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public KcAckHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.KC_ACK) {
            AppConfig.timestampedErrorPrint("KcAckHandler got wrong message type: " + clientMessage);
            return;
        }

        int senderId = clientMessage.getOriginalSenderInfo().getId();
        String text = clientMessage.getMessageText();
        int amount = Integer.parseInt(text);

        KcSnapshotResult result = new KcSnapshotResult(senderId, amount);
        snapshotCollector.addKcSnapshotInfo(senderId, result);

        AppConfig.timestampedStandardPrint("KcAckHandler: " + clientMessage.getOriginalSenderInfo().getId() + " sent ack with amount: " + amount);
    }
}
