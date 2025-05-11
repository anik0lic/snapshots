package cli.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotType;
import servent.message.CausalBroadcastShared;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 10;
	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	private BitcakeManager bitcakeManager;
	private SnapshotCollector snapshotCollector;
	
	public TransactionBurstCommand(BitcakeManager bitcakeManager) {
		this.bitcakeManager = bitcakeManager;
	}

	public TransactionBurstCommand(SnapshotCollector snapshotCollector, BitcakeManager bitcakeManager) {
		this.snapshotCollector = snapshotCollector;
		this.bitcakeManager = bitcakeManager;
	}
	
	private class TransactionBurstWorker implements Runnable {
		
		@Override
		public void run() {
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
					ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);
					
					int amount = 1 + rand.nextInt(MAX_TRANSFER_AMOUNT);

					Message transactionMessage;
					
					/*
					 * The message itself will reduce our bitcake count as it is being sent.
					 * The sending might be delayed, so we want to make sure we do the
					 * reducing at the right time, not earlier.
					 */
					Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

//					if (AppConfig.SNAPSHOT_TYPE == SnapshotType.ACHARYA_BADRINATH) {
					transactionMessage = new TransactionMessage(
							AppConfig.myServentInfo, neighborInfo, amount, bitcakeManager, vectorClock);

					MessageUtil.sendMessage(transactionMessage);
//					}
//					else {
//						transactionMessage = new TransactionMessage(
//								AppConfig.myServentInfo, neighborInfo, amount, bitcakeManager, vectorClock);
//						MessageUtil.sendMessage(transactionMessage);
//					}
				}
				
			}
		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			
			t.start();
		}
	}

	
}
