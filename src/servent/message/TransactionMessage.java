package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.acharya_badrinath.AbBitcakeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends CausalMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;
	
//	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager) {
//		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
//		this.bitcakeManager = bitcakeManager;
//	}

	private TransactionMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
							   List<ServentInfo> routeList, String messageText,
							   int messageId, Map<Integer, Integer> senderVectorClock, BitcakeManager bitcakeManager) {

		super(MessageType.TRANSACTION, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);
		this.bitcakeManager = bitcakeManager;

	}

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager, Map<Integer, Integer> vectorClock) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount), vectorClock);
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public Message makeMeASender() {
		ServentInfo myInfo = AppConfig.myServentInfo;
		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(myInfo);
		Message toReturn = new TransactionMessage(getOriginalSenderInfo(), getReceiverInfo(), newRouteList,
				getMessageText(), getMessageId(), getSenderVectorClock(), this.bitcakeManager);

		return toReturn;
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			Message toReturn = new TransactionMessage(getOriginalSenderInfo(), newReceiverInfo,
					getRoute(), getMessageText(), getMessageId(), getSenderVectorClock(), this.bitcakeManager);

			return toReturn;
		} else {
			AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
			return null;
		}
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		if(bitcakeManager != null) {
			int amount = Integer.parseInt(getMessageText());

			bitcakeManager.takeSomeBitcakes(amount);

			if (bitcakeManager instanceof AbBitcakeManager) {
				AbBitcakeManager abBitcakeManager = (AbBitcakeManager) bitcakeManager;

				abBitcakeManager.recordSentTransaction(getReceiverInfo().getId(), this);
			}
		}
	}
}
