package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.SnapshotCollector;

import java.util.List;
import java.util.Map;

public class CausalMessage extends BasicMessage {

    private static final long serialVersionUID = 3285425020473654585L;

    private final Map<Integer, Integer> senderVectorClock;

    public CausalMessage(MessageType messageType, ServentInfo senderInfo,
                         ServentInfo receiverInfo, String messageText,
                         Map<Integer, Integer> senderVectorClock) {

        super(messageType, senderInfo, receiverInfo, messageText);

        this.senderVectorClock = senderVectorClock;

    }

    protected CausalMessage(MessageType messageType, ServentInfo originalSenderInfo,
                            ServentInfo receiverInfo, List<ServentInfo> routeList,
                            String messageText, int messageId,
                            Map<Integer, Integer> senderVectorClock) {

        super(messageType, originalSenderInfo, receiverInfo, routeList, messageText, messageId);

        this.senderVectorClock = senderVectorClock;

    }

    public Map<Integer, Integer> getSenderVectorClock() { return senderVectorClock; }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new CausalMessage(getMessageType(), getOriginalSenderInfo(),
                getReceiverInfo(), newRouteList, getMessageText(), getMessageId(), getSenderVectorClock());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new CausalMessage(getMessageType(), getOriginalSenderInfo(),
                    newReceiverInfo, getRoute(), getMessageText(), getMessageId(), getSenderVectorClock());

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
            return null;
        }

    }

    @Override
    public void sendEffect() {}

}
