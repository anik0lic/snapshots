package servent.handler.snapshot.coordinated_checkpointing;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.coordinated_checkpointing.KcBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class KcResumeHandler implements MessageHandler {
    private final Message clientMessage;
    private final BitcakeManager bitcakeManager;

    public KcResumeHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = snapshotCollector.getBitcakeManager();
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.KC_RESUME) {
            AppConfig.timestampedErrorPrint("Received message of type " + clientMessage.getMessageType() + ", but expected KC_RESUME.");
            return;
        }
        AppConfig.timestampedStandardPrint("Received resume message from: " + clientMessage.getOriginalSenderInfo().getId());

        if (bitcakeManager instanceof KcBitcakeManager) {
            ((KcBitcakeManager) bitcakeManager).handleResume();
        }
    }
}
