package servent.message.snapshot.acharya_badrinath;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.acharya_badrinath.AbSnapshotResult;
import servent.message.BasicMessage;
import servent.message.CausalMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class AbResultMessage extends CausalMessage {
    private static final long serialVersionUID = -2849374084192942310L;
    private final AbSnapshotResult result;

    public AbResultMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock, AbSnapshotResult result) {
        super(MessageType.AB_RESULT, sender, receiver, "", senderVectorClock);
        this.result = result;
        AppConfig.timestampedStandardPrint("Vector clock: " + senderVectorClock);
    }

    public AbResultMessage(ServentInfo sender, ServentInfo receiver, List<ServentInfo> routeList, String messageText, int messageId,
                           Map<Integer, Integer> senderVectorClock, AbSnapshotResult result) {
        super(MessageType.AB_RESULT, sender, receiver, routeList, messageText, messageId, senderVectorClock);
        this.result = result;
        AppConfig.timestampedStandardPrint("Vector clock: " + senderVectorClock);
    }

    public AbSnapshotResult getABSnapshotResult() { return result; }

    @Override
    public Message makeMeASender() {
        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);

        Message toReturn = new AbResultMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock(), result);

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new AbResultMessage(getOriginalSenderInfo(), newReceiverInfo,
                    getRoute(), getMessageText(), getMessageId(), getSenderVectorClock(), result);

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
            return null;
        }

    }

}
