package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotType;
import servent.handler.*;
import servent.handler.snapshot.acharya_badrinath.AbResultHandler;
import servent.handler.snapshot.acharya_badrinath.AbTokenHandler;
import servent.handler.snapshot.alagar_venkatesan.AvDoneHandler;
import servent.handler.snapshot.alagar_venkatesan.AvTerminateHandler;
import servent.handler.snapshot.alagar_venkatesan.AvTokenHandler;
import servent.handler.snapshot.coordinated_checkpointing.KcAckHandler;
import servent.handler.snapshot.coordinated_checkpointing.KcRequestHandler;
import servent.handler.snapshot.coordinated_checkpointing.KcResumeHandler;
import servent.message.CausalBroadcastShared;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	private List<Message> redMessages = new ArrayList<>();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;

				/*
				 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
				 */
				Socket clientSocket = listenerSocket.accept();

				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
					case TRANSACTION:
						messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
						break;

					case KC_ACK:
						messageHandler = new KcAckHandler(clientMessage, snapshotCollector);
						break;
					case KC_RESUME:
						messageHandler = new KcResumeHandler(clientMessage, snapshotCollector);
						break;
					case KC_REQUEST:
						messageHandler = new KcRequestHandler(clientMessage, snapshotCollector);
						break;

					case AB_RESULT:
						messageHandler = new AbResultHandler(clientMessage, snapshotCollector);
						break;
					case AB_TOKEN:
						messageHandler = new AbTokenHandler(clientMessage, snapshotCollector);
						break;

					case AV_TOKEN:
						messageHandler = new AvTokenHandler(clientMessage, snapshotCollector);
						break;
					case AV_DONE:
						messageHandler = new AvDoneHandler(clientMessage, snapshotCollector);
						break;
					case AV_TERMINATE:
						messageHandler = new AvTerminateHandler(clientMessage, snapshotCollector);
						break;

					case POISON:
						break;
				}

				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
