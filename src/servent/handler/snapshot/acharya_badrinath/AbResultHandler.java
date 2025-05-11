package servent.handler.snapshot.acharya_badrinath;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.acharya_badrinath.AbBitcakeManager;
import app.snapshot_bitcake.acharya_badrinath.AbSnapshotResult;
import servent.handler.MessageHandler;
import servent.message.CausalBroadcastShared;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.acharya_badrinath.AbResultMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AbResultHandler implements MessageHandler {
    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public AbResultHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.AB_RESULT) {
            AppConfig.timestampedErrorPrint("AbResultnHandler got wrong message type: " + clientMessage);
            return;
        }

        if (snapshotCollector.isCollecting()) {
            AbResultMessage abResultMessage = (AbResultMessage) clientMessage;
            snapshotCollector.addAbSnapshotInfo(
                    abResultMessage.getOriginalSenderInfo().getId(),
                    abResultMessage.getABSnapshotResult()
            );
        }
//        AbResultMessage abResultMessage = (AbResultMessage) clientMessage;
//        snapshotCollector.addAbSnapshotInfo(
//                abResultMessage.getOriginalSenderInfo().getId(),
//                abResultMessage.getABSnapshotResult()
//        );
    }
}
