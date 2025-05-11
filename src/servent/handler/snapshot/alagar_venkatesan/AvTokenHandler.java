package servent.handler.snapshot.alagar_venkatesan;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.alagar_venkatesan.AvBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class AvTokenHandler implements MessageHandler {
    private Message clientMessage;
    private BitcakeManager bitcakeManager;
    private SnapshotCollector snapshotCollector;

    public AvTokenHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = snapshotCollector.getBitcakeManager();
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.AV_TOKEN) {
            ((AvBitcakeManager)bitcakeManager).handleToken(clientMessage, snapshotCollector);
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }
    }
}
