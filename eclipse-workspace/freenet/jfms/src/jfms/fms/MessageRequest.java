package jfms.fms;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jfms.fcp.FcpRequest;
import jfms.fms.xml.MessageParser;

public class MessageRequest extends FcpRequest {
	private static final Logger LOG = Logger.getLogger(MessageRequest.class.getName());
	private static final MessageParser messageParser = new MessageParser();
	private static final Pattern UUIDREPLACE_PATTERN = Pattern.compile("[~-]");

	private final MessageReference messageRef;

	public MessageRequest(String id, MessageReference msg) {
		super(id, Identity.getMessageKey(msg.getSsk(), msg.getDate(), msg.getIndex()));
		this.messageRef = msg;
	}

	@Override
	public void finished(byte[] data) {
		Message message = messageParser.parse(new ByteArrayInputStream(data));
		if (message == null) {
			LOG.log(Level.WARNING, "failed to parse message from {0}",
					getKey());
			return;
		}

		LOG.log(Level.FINE, "retrieved Message in Board {0} "
				+ "with Subject {1}", new Object[]{
				message.getReplyBoard(), message.getSubject()});
		message.setIdentityId(messageRef.getIdentityId());
		message.setInsertDate(messageRef.getDate());
		message.setInsertIndex(messageRef.getIndex());

		final String expectedHash = UUIDREPLACE_PATTERN
			.matcher(messageRef.getPublicKeyHash())
			.replaceAll("");

		final String uuid = message.getMessageUuid();
		final int uuidLen = uuid.length();
		int startPos = uuidLen - expectedHash.length();
		if (startPos < 0) {
			startPos = 0;
		}

		final String receivedHash = uuid.substring(startPos, uuidLen);
		if (!expectedHash.equals(receivedHash)) {
			LOG.log(Level.WARNING, "invalid message UUID. expected: {0} "
				+ "received: {1}", new Object[]{expectedHash, receivedHash});
			return;
		}


		Store store = FmsManager.getInstance().getStore();
		int messageId = store.insertMessage(message);
		if (messageId != -1) {
			message.setMessageId(messageId);

			MessageManager messageManager = FmsManager.getInstance().getMessageManager();
			messageManager.addMessage(message);
		}
	}
};
