package servent.handler.snapshot.alagar_venkatesan;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.alagar_venkatesan.AvBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class AvTerminateHandler implements MessageHandler {
    private Message clientMessage;
    private BitcakeManager bitcakeManager;
    private SnapshotCollector snapshotCollector;

    public AvTerminateHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = snapshotCollector.getBitcakeManager();
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.AV_TERMINATE) {
            ((AvBitcakeManager)bitcakeManager).handleTerminate(clientMessage, snapshotCollector);
        } else {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }
    }
}
