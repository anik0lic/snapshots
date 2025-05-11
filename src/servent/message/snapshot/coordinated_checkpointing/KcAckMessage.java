package servent.message.snapshot.coordinated_checkpointing;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class KcAckMessage extends BasicMessage {

    private static final long serialVersionUID = -138936178857275482L;


    public KcAckMessage(ServentInfo sender, ServentInfo receiver, String message) {
        super(MessageType.KC_ACK, sender, receiver, message);
    }
}

