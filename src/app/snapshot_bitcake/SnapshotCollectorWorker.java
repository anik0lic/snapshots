package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.acharya_badrinath.AbBitcakeManager;
import app.snapshot_bitcake.acharya_badrinath.AbSnapshotResult;
import app.snapshot_bitcake.alagar_venkatesan.AvBitcakeManager;
import app.snapshot_bitcake.alagar_venkatesan.AvSnapshotResult;
import app.snapshot_bitcake.coordinated_checkpointing.KcBitcakeManager;
import app.snapshot_bitcake.coordinated_checkpointing.KcSnapshotResult;
import servent.message.Message;
import servent.message.snapshot.coordinated_checkpointing.KcRequestMessage;
import servent.message.snapshot.coordinated_checkpointing.KcResumeMessage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);

	private Map<Integer, KcSnapshotResult> collectedKcValues = new ConcurrentHashMap<>();
	private Map<Integer, AbSnapshotResult> collectedAbValues = new ConcurrentHashMap<>();
	private Map<Integer, AvSnapshotResult> collectedAvValues = new ConcurrentHashMap<>();

	private SnapshotType snapshotType;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
		
		switch(snapshotType) {
			case COORDINATED_CHECKPOINTING:
				 this.bitcakeManager = new KcBitcakeManager();
				break;

			case ACHARYA_BADRINATH:
				this.bitcakeManager = new AbBitcakeManager();
				break;

			case ALAGAR_VENKATESAN:
				this.bitcakeManager = new AvBitcakeManager();
				break;

			case NONE:
				AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
				System.exit(0);
			}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (!collecting.get()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!working) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			switch (snapshotType) {
				case COORDINATED_CHECKPOINTING:
					AppConfig.timestampedStandardPrint("Starting COORDINATED_CHECKPOINTING snapshot.");
					((KcBitcakeManager) bitcakeManager).handleRequest(new KcRequestMessage(AppConfig.myServentInfo, AppConfig.myServentInfo), this);
					break;

				case ACHARYA_BADRINATH:
					AppConfig.timestampedStandardPrint("Starting ACHARYA_BADRINATH snapshot.");
					((AbBitcakeManager) bitcakeManager).handleRequest(this);
					break;

				case ALAGAR_VENKATESAN:
					AppConfig.timestampedStandardPrint("Starting ALAGAR_VENKATESAN snapshot.");
					((AvBitcakeManager) bitcakeManager).handleRequest(this);
					break;

				case NONE:
					//Shouldn't be able to come here. See constructor.
					break;
			}
			
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
					case COORDINATED_CHECKPOINTING:
						if (collectedKcValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
						break;

					case ACHARYA_BADRINATH:
						if (collectedAbValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
						break;

					case ALAGAR_VENKATESAN:
						if (collectedAvValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
						break;

					case NONE:
						//Shouldn't be able to come here. See constructor.
						break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!working) {
					return;
				}
			}
			
			//print
			int totalBitcakes;
			switch (snapshotType) {
				case COORDINATED_CHECKPOINTING:
					AppConfig.timestampedStandardPrint("Coordinated Checkpointing Snapshot Results:");
					totalBitcakes = 0;
					for (Entry<Integer, KcSnapshotResult> entry : collectedKcValues.entrySet()) {
						totalBitcakes += entry.getValue().getRecordedAmount();
						AppConfig.timestampedStandardPrint("Servent " + entry.getKey() + ": " + entry.getValue().getRecordedAmount() + " bitcakes");
					}
					AppConfig.timestampedStandardPrint("Total bitcakes in system (KC): " + totalBitcakes);

					AppConfig.timestampedStandardPrint("KC Snapshot complete. Sending RESUME to neighbors.");
					for (int i = 0; i < AppConfig.getServentCount(); i++) {
						if (i == AppConfig.myServentInfo.getId()) {
							if (bitcakeManager instanceof KcBitcakeManager) {
								((KcBitcakeManager) bitcakeManager).handleResume();
							}
							continue;
						}
						ServentInfo serventInfo = AppConfig.getInfoById(i);
						Message kcResumeMessage = new KcResumeMessage(AppConfig.myServentInfo, serventInfo);
						MessageUtil.sendMessage(kcResumeMessage);
					}

					collectedKcValues.clear();
					break;

				case ACHARYA_BADRINATH:
					AppConfig.timestampedStandardPrint("Acharya-Badrinath Snapshot Results:");
					totalBitcakes = 0;
					for(Entry<Integer, AbSnapshotResult> entry : collectedAbValues.entrySet()) {
						totalBitcakes += entry.getValue().getRecordedAmount();
						AppConfig.timestampedStandardPrint("Servent " + entry.getKey() + ": " + entry.getValue().getRecordedAmount() + " bitcakes");
					}

					for (int i = 0; i < AppConfig.getServentCount(); i++) {
						for (int j = 0; j < AppConfig.getServentCount(); j++) {
							if (i != j) {
								if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
										AppConfig.getInfoById(j).getNeighbors().contains(i)) {
									List<Message> sent = collectedAbValues.get(i).getSent().get(j);
									List<Message> received = collectedAbValues.get(j).getReceived().get(i);

									if (sent.size() != received.size()) {
										int unreceived = Math.abs(sent.size() - received.size());
										AppConfig.timestampedStandardPrint("Unreceived amounts between " + i + " and " + j + ": " + unreceived);
										totalBitcakes += unreceived;
									}
								}
							}
						}
					}

					AppConfig.timestampedStandardPrint("Total bitcakes in system (AB): " + totalBitcakes);
					((AbBitcakeManager) bitcakeManager).resetSnapshotState();
					collectedAbValues.clear();
					break;

				case ALAGAR_VENKATESAN:
					AppConfig.timestampedStandardPrint("Alagar-Venkatesan Snapshot Results:");
					totalBitcakes = 0;
					for (Entry<Integer, AvSnapshotResult> entry : collectedAvValues.entrySet()) {
						totalBitcakes += entry.getValue().getRecordedAmount();
						AppConfig.timestampedStandardPrint("Servent " + entry.getKey() + ": " + entry.getValue().getRecordedAmount() + " bitcakes");
					}
					AppConfig.timestampedStandardPrint("Total bitcakes in system (AV): " + totalBitcakes);

					collectedAvValues.clear();
					break;

				case NONE:
					//Shouldn't be able to come here. See constructor.
					break;
				}
			collecting.set(false);
		}

	}

	@Override
	public void addKcSnapshotInfo(int id, KcSnapshotResult kcSnapshotResult) {
		collectedKcValues.put(id, kcSnapshotResult);
	}

	@Override
	public void addAbSnapshotInfo(int id, AbSnapshotResult abSnapshotResult) {
		collectedAbValues.put(id, abSnapshotResult);
	}

	@Override
	public void addAvSnapshotInfo(int id, AvSnapshotResult avSnapshotResult) {
		collectedAvValues.put(id, avSnapshotResult);
	}


	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}

	@Override
	public boolean isCollecting() { return collecting.get(); }
	
	@Override
	public void stop() {
		working = false;
	}

}
