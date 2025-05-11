package servent.handler.snapshot.acharya_badrinath;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.acharya_badrinath.AbBitcakeManager;
import app.snapshot_bitcake.coordinated_checkpointing.KcBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class AbTokenHandler implements MessageHandler {
    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public AbTokenHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.AB_TOKEN) {
            AppConfig.timestampedErrorPrint("AbTokenHandler got wrong message type: " + clientMessage);
            return;
        }

        if (snapshotCollector.getBitcakeManager() instanceof AbBitcakeManager) {
            AbBitcakeManager abBitcakeManager = (AbBitcakeManager) snapshotCollector.getBitcakeManager();
            abBitcakeManager.handleToken(clientMessage, snapshotCollector);
        }
    }
}
