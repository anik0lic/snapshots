package servent.message.snapshot.coordinated_checkpointing;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class KcRequestMessage extends BasicMessage {
    private static final long serialVersionUID = -723936178857275482L;


    public KcRequestMessage(ServentInfo sender, ServentInfo reciever) {
        super(MessageType.KC_REQUEST, sender, reciever);
    }
}
