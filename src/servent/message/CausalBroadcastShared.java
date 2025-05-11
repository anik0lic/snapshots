package servent.message;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.acharya_badrinath.AbResultHandler;
import servent.handler.snapshot.acharya_badrinath.AbTokenHandler;
import servent.handler.snapshot.alagar_venkatesan.AvDoneHandler;
import servent.handler.snapshot.alagar_venkatesan.AvTerminateHandler;
import servent.handler.snapshot.alagar_venkatesan.AvTokenHandler;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 *
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {
    private static Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
    private static List<Message> commitedCausalMessageList = new CopyOnWriteArrayList<>();

    private static Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private static Object pendingMessagesLock = new Object();

    private static final ExecutorService handlerThreadPool = Executors.newWorkStealingPool();

//    private static final List<Message> sendTransactions = new CopyOnWriteArrayList<>();
//    private static final List<Message> receivedTransactions = new CopyOnWriteArrayList<>();

    private static final Object cLock = new Object();

    public static void initializeVectorClock(int serventCount) {
        synchronized (cLock) {
            for (int i = 0; i < serventCount; i++) {
                vectorClock.put(i, 0);
            }
        }
        AppConfig.timestampedStandardPrint("Vector clock initialized: " + vectorClock);
    }

    public static void incrementClock(int serventId) {
        synchronized (cLock) {
            vectorClock.computeIfPresent(serventId, new BiFunction<Integer, Integer, Integer>() {

                @Override
                public Integer apply(Integer key, Integer oldValue) {
                    return oldValue + 1;
                }
            });
        }
    }

    public static Map<Integer, Integer> getVectorClock() {
        Map<Integer, Integer> toReturn = new HashMap<>();
        synchronized (cLock) {
            for (Map.Entry<Integer, Integer> m : vectorClock.entrySet()) {
                toReturn.put(m.getKey(), m.getValue());
            }
        }

        return toReturn;
    }

    public static List<Message> getCommitedCausalMessages() {
        List<Message> toReturn = new CopyOnWriteArrayList<>(commitedCausalMessageList);

        return toReturn;
    }

    public static void addPendingMessage(Message msg) {
        pendingMessages.add(msg);
    }

    public static void commitCausalMessage(Message newMessage, SnapshotCollector snapshotCollector) {
        AppConfig.timestampedStandardPrint("Committing " + newMessage);
        commitedCausalMessageList.add(newMessage);
        incrementClock(newMessage.getOriginalSenderInfo().getId());

        checkPendingMessages(snapshotCollector);
    }

    public static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
        if (clock1.size() != clock2.size()) {
            throw new IllegalArgumentException("Clocks are not same size how why");
        }

        for(int i = 0; i < clock1.size(); i++) {
            if (clock2.get(i) > clock1.get(i)) {
                return true;
            }
        }

        return false;
    }

    public static void checkPendingMessages(SnapshotCollector snapshotCollector) {
        boolean gotWork = true;

        while (gotWork) {
            gotWork = false;

            synchronized (pendingMessagesLock) {
                Iterator<Message> iterator = pendingMessages.iterator();
                Map<Integer, Integer> myVectorClock = getVectorClock();

                while (iterator.hasNext()) {
                    Message message = iterator.next();
                    CausalMessage pendingMessage = (CausalMessage) message;

                    if (!otherClockGreater(myVectorClock, pendingMessage.getSenderVectorClock())) {
                        gotWork = true;

                        commitedCausalMessageList.add(pendingMessage);
                        incrementClock(pendingMessage.getOriginalSenderInfo().getId());

                        MessageHandler messageHandler = new NullHandler(pendingMessage);

                        switch (pendingMessage.getMessageType()) {
                            case TRANSACTION:
                                if (pendingMessage.getReceiverInfo().getId() == AppConfig.myServentInfo.getId()) {
                                    messageHandler = new TransactionHandler(pendingMessage, snapshotCollector.getBitcakeManager());
                                }
                                break;

                            case AB_TOKEN:
                                AppConfig.timestampedStandardPrint("Processing token: " + pendingMessage);
                                messageHandler = new AbTokenHandler(pendingMessage, snapshotCollector);
                                break;
                            case AB_RESULT:
                                AppConfig.timestampedStandardPrint("Processing result: " + pendingMessage);
                                if (pendingMessage.getReceiverInfo().getId() == AppConfig.myServentInfo.getId()) {
                                    messageHandler = new AbResultHandler(pendingMessage, snapshotCollector);
                                }
                                break;

                            case AV_TOKEN:
                                AppConfig.timestampedStandardPrint("Processing token: " + pendingMessage);
                                messageHandler = new AvTokenHandler(pendingMessage, snapshotCollector);
                                break;
                            case AV_DONE:
                                AppConfig.timestampedStandardPrint("Processing done: " + pendingMessage);
                                messageHandler = new AvDoneHandler(pendingMessage, snapshotCollector);
                                break;
                            case AV_TERMINATE:
                                AppConfig.timestampedStandardPrint("Processing terminate: " + pendingMessage);
                                messageHandler = new AvTerminateHandler(pendingMessage, snapshotCollector);
                                break;
                        }
                        handlerThreadPool.submit(messageHandler);

                        iterator.remove();
                        break;
                    }
                }
            }
        }

    }

    public static void stop(){ handlerThreadPool.shutdown(); }

    public static Object getPendingMessagesLock() {
        return pendingMessagesLock;
    }

    public static List<Message> getPendingMessages() {
        return new CopyOnWriteArrayList<>(pendingMessages);
    }
}
