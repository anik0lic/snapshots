package servent.handler;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.acharya_badrinath.AbBitcakeManager;
import servent.message.Message;
import servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);

			if (bitcakeManager instanceof AbBitcakeManager) {
				AbBitcakeManager abBitcakeManager = (AbBitcakeManager) bitcakeManager;
				abBitcakeManager.recordReceivedTransaction(clientMessage.getOriginalSenderInfo().getId(), clientMessage);
			}
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
