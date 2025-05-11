package servent.handler.snapshot.coordinated_checkpointing;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.coordinated_checkpointing.KcBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class KcRequestHandler implements MessageHandler {
    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public KcRequestHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.KC_REQUEST) {
            System.err.println("Received message of type " + clientMessage.getMessageType() + " in KcRequestHandler");
            return;
        }

        AppConfig.timestampedStandardPrint("Received KcRequest message from: " + clientMessage.getOriginalSenderInfo().getId());

        if (snapshotCollector.getBitcakeManager() instanceof KcBitcakeManager) {
            KcBitcakeManager kcBitcakeManager = (KcBitcakeManager) snapshotCollector.getBitcakeManager();
            kcBitcakeManager.handleRequest(clientMessage, snapshotCollector);
        }
    }
}
