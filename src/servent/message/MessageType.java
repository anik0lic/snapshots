package servent.message;

public enum MessageType {
	POISON, TRANSACTION,

	KC_REQUEST, KC_ACK, KC_RESUME,

	AB_TOKEN, AB_RESULT,

	AV_DONE, AV_TERMINATE, AV_TOKEN,
}
