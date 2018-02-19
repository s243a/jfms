package jfms.fms;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jfms.fcp.FcpRequest;
import jfms.fms.xml.MessageListParser;

public class MessageListRequest extends FcpRequest {
	private static final Logger LOG = Logger.getLogger(MessageListRequest.class.getName());
	private static final Pattern MSGLIST_PATTERN = Pattern.compile("USK@.*MessageList/(\\d+)/MessageList.xml");

	private final int identityId;
	private final String ssk;
	private final LocalDate date;
	private int index;
	private final MessageReferenceList messageRequests;

	public MessageListRequest(String id, int identityId, String ssk,
		LocalDate date, int index,
		MessageReferenceList messageRequests
	) {
		super(id, Identity.getMessageListKey(ssk, date, index, true));
		this.identityId = identityId;
		this.ssk = ssk;
		this.date = date;
		this.index = index;
		this.messageRequests = messageRequests;
	}

	@Override
	public void finished(byte[] data) {
		MessageListParser messageListParser = new MessageListParser();
		List<MessageReference> messageList = messageListParser.parse(new ByteArrayInputStream(data));

		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
		Identity id = identityManager.getIdentity(identityId);

		LOG.log(Level.FINEST, "Retrieved MessageList of {0} containing "
				+ "{1} messages", new Object[]{
				id.getFullName(), messageList.size()});

		for (MessageReference m : messageList) {
			final String msgSsk = m.getSsk();
			if (msgSsk == null) {
				m.setIdentityId(identityId);
				m.setSsk(ssk);
			} else {
				Integer msgIdentityId = identityManager.getIdentityId(msgSsk);
				if (msgIdentityId == null) {
					LOG.log(Level.FINEST, "Skipping unknown ID {0} "
							+ "in MessageList", msgSsk);
					continue;
				}
				m.setIdentityId(msgIdentityId);
			}

			messageRequests.addMessageToDownload(m);
		}

		messageRequests.messageListFinished(identityId, date, index);
	}

	@Override
	public boolean redirect(String redirectURI) {
		Matcher m = MSGLIST_PATTERN.matcher(redirectURI);
		if (m.matches() && m.groupCount() > 0) {
			index = Integer.parseInt(m.group(1));
			LOG.log(Level.FINEST, "got redirect, updating index to {0}", index);
		} else {
			LOG.log(Level.WARNING, "failed to parse redirectURI");
		}

		setKey(redirectURI);

		return true;
	}
}
