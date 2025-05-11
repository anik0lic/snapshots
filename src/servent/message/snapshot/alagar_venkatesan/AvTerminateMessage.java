package servent.message.snapshot.alagar_venkatesan;

import app.AppConfig;
import app.ServentInfo;
import servent.message.CausalMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class AvTerminateMessage extends CausalMessage {
    private static final long serialVersionUID = 865832214162997L;

    public AvTerminateMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AV_TERMINATE, senderInfo, receiverInfo, "", senderVectorClock);

    }

    private AvTerminateMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                               List<ServentInfo> routeList, String messageText,
                               int messageId, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AV_TERMINATE, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

    }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new AvTerminateMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new AvTerminateMessage(getOriginalSenderInfo(), newReceiverInfo,
                    getRoute(), getMessageText(), getMessageId(), getSenderVectorClock());

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
            return null;
        }

    }

    @Override
    public void sendEffect() {}
}
