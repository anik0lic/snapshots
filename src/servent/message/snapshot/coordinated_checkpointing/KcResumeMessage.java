package servent.message.snapshot.coordinated_checkpointing;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class KcResumeMessage extends BasicMessage {

    private static final long serialVersionUID = -4947221171569980641L;

    public KcResumeMessage(ServentInfo sender, ServentInfo receiver) {
        super(MessageType.KC_RESUME, sender, receiver);
    }
}

