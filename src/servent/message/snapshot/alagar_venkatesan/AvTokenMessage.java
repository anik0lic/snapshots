package servent.message.snapshot.alagar_venkatesan;

import app.AppConfig;
import app.ServentInfo;
import servent.message.CausalMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class AvTokenMessage extends CausalMessage {
    private static final long serialVersionUID = -643645745747232L;

    private final ServentInfo collector;

    public AvTokenMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock, ServentInfo collector) {
        super(MessageType.AV_TOKEN, sender, receiver, "", senderVectorClock);

        this.collector = collector;
    }

    public AvTokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                          List<ServentInfo> routeList, String messageText,
                          int messageId, Map<Integer, Integer> senderVectorClock, ServentInfo collector) {

        super(MessageType.AV_TOKEN, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

        this.collector = collector;
    }

    public ServentInfo getCollector() {
        return collector;
    }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new AvTokenMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock(), getCollector());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new AvTokenMessage(getOriginalSenderInfo(), newReceiverInfo,
                    getRoute(), getMessageText(), getMessageId(), getSenderVectorClock(), getCollector());

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
            return null;
        }

    }

    @Override
    public void sendEffect() {}
}
