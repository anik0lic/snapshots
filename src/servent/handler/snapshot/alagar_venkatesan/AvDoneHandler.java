package servent.handler.snapshot.alagar_venkatesan;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.alagar_venkatesan.AvBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class AvDoneHandler implements MessageHandler {
    private Message clientMessage;
    private SnapshotCollector snapshotCollector;
    private BitcakeManager bitcakeManager;


    public AvDoneHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
        this.bitcakeManager = snapshotCollector.getBitcakeManager();
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.AV_DONE) {
//                snapshotCollector.markServentAsDone(clientMessage.getOriginalSenderInfo().getId());
            ((AvBitcakeManager)bitcakeManager).handleDone(clientMessage, snapshotCollector);
        } else {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }
    }
}
