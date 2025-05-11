package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.SnapshotCollector;
import servent.message.CausalBroadcastShared;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CausalMessageHandler implements MessageHandler {
    private static final boolean MESSAGE_UTIL_PRINTING = false;

    private Message clientMessage;
    private Set<Message> receivedMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private SnapshotCollector snapshotCollector;

    public CausalMessageHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        try {
            ServentInfo sender = clientMessage.getOriginalSenderInfo();

            if (sender.getId() != AppConfig.myServentInfo.getId()) {
                synchronized (AppConfig.lock) {
                    boolean isNew = receivedMessages.add(clientMessage);

                    if (isNew) {
                        ServentInfo lastSender = clientMessage.getRoute().size() == 0 ?
                                clientMessage.getOriginalSenderInfo() :
                                clientMessage.getRoute().get(clientMessage.getRoute().size() - 1);

                        if (MESSAGE_UTIL_PRINTING)
                            AppConfig.timestampedStandardPrint("Rebroadcasting " + clientMessage.getMessageType() + " message.");

//                        CausalBroadcastShared.addPendingMessage(clientMessage);
//                        CausalBroadcastShared.checkPendingMessages(snapshotCollector);

//                        for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
//                            if (neighbor != sender.getId()) {
//                                MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
//                            }
//                        }

                        for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                            if (lastSender.getId() != neighbor) {
                                MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
                            }
                        }

                        CausalBroadcastShared.addPendingMessage(clientMessage);
                        CausalBroadcastShared.checkPendingMessages(snapshotCollector);
                    } else {
                        if (MESSAGE_UTIL_PRINTING)
                            AppConfig.timestampedStandardPrint("Already had this " + clientMessage.getMessageType() + " message. No rebroadcast.");
                    }
                }
            } else {
                if (MESSAGE_UTIL_PRINTING)
                    AppConfig.timestampedStandardPrint("Got own " + clientMessage.getMessageType() + " message back. No rebroadcast.");
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("Error in CausalMessageHandler: " + e.getMessage());
        }
    }
}
