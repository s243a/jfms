package jfms.frost;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.fcp.FcpRequest;

public class MessageRequest extends FcpRequest {
	private static final Logger LOG = Logger.getLogger(MessageRequest.class.getName());
	private static final MessageParser messageParser = new MessageParser();

	private final RequestInfo requestInfo;

	public MessageRequest(String id, RequestInfo requestInfo) {
		super(id, requestInfo.getMessageKey());
		this.requestInfo = requestInfo;
	}

	@Override
	public void finished(byte[] data) {
		Message message = messageParser.parse(new ByteArrayInputStream(data));
		LOG.log(Level.FINE, "retrieved FrostMessage with Subject {0} {1}", new Object[]{message.getSubject(), message.getDate()});
	}

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}
};
