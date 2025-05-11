package servent.message.snapshot.acharya_badrinath;

import app.AppConfig;
import app.ServentInfo;
import servent.message.CausalMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class AbTokenMessage extends CausalMessage {
    private static final long serialVersionUID = 7296136760513950171L;

    public AbTokenMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AB_TOKEN, sender, receiver, "", senderVectorClock);
        AppConfig.timestampedStandardPrint("Vector clock: " + senderVectorClock);
    }

    private AbTokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                           List<ServentInfo> routeList, String messageText,
                           int messageId, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AB_TOKEN, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

    }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new AbTokenMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new AbTokenMessage(getOriginalSenderInfo(), newReceiverInfo,
                    getRoute(), getMessageText(), getMessageId(), getSenderVectorClock());

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
            return null;
        }

    }

}
