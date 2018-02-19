package jfms.fms;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.config.Config;
import jfms.config.Constants;

public class MessageReferenceList {
	private static final Logger LOG = Logger.getLogger(MessageReferenceList.class.getName());

	private final Set<MessageReference> messagesToDownload = new HashSet<>();
	private final List<MessageListReference> messageListsFinished = new ArrayList<>();

	MessageReferenceList() {
	}

	public void addMessageToDownload(MessageReference message) {
		TrustManager trustManager = FmsManager.getInstance().getTrustManager();

		// List of boards in the message list may be used to decided whether we
		// download a message. The message itself contains the authorative list
		// of boards.

		final int identityId = message.getIdentityId();
		Integer peerMessageTrust = trustManager.getPeerMessageTrust(identityId);

		boolean isTrusted;
		if (Config.getInstance().getDownloadMessagesWithoutTrust()) {
			isTrusted = peerMessageTrust == null ||
				peerMessageTrust >= Constants.MIN_PEER_MESSSAGE_TRUST;
		} else {
			isTrusted = peerMessageTrust != null &&
				peerMessageTrust >= Constants.MIN_PEER_MESSSAGE_TRUST;
		}

		if (isTrusted) {
			// Clear the list of boards to make sure we won't have duplicates
			// in messagesToDownload that differ only in boards
			message.getBoards().clear();

			messagesToDownload.add(message);
		}
	}

	public List<MessageListReference> getMessageListsFinished() {
		return messageListsFinished;
	}

	public void messageListFinished(int identityId, LocalDate date, int index) {
		messageListsFinished.add(new MessageListReference(identityId, date, index));
	}

	public int size() {
		return messagesToDownload.size();
	}

	public boolean isEmpty() {
		return messagesToDownload.isEmpty();
	}

	public void cleanup() {
		Store store = FmsManager.getInstance().getStore();

		LocalDate oldestMessageDate = LocalDate.now(ZoneOffset.UTC)
			.minusDays(Constants.MAX_MESSAGE_AGE);

		int oldCount = 0;
		int existsCount = 0;
		int totalCount = messagesToDownload.size();

		Iterator<MessageReference> iter = messagesToDownload.iterator();
		while (iter.hasNext()) {
			MessageReference msg = iter.next();

			boolean remove = false;
			if (msg.getDate().compareTo(oldestMessageDate) < 0) {
				oldCount++;
				remove = true;
			}

			if (!remove && store.messageExists(
						msg.getIdentityId(),
						msg.getDate(),
						msg.getIndex())) {
				existsCount++;
				remove = true;
			}

			if (remove) {
				iter.remove();
			}
		}

		LOG.log(Level.FINER, "Found {0} messages: "
				+ "{1} new, {2} too old, {3} already exist", new Object[]{
				totalCount, messagesToDownload.size(), oldCount, existsCount});
	}

	public MessageReference remove() {
		Iterator<MessageReference> iter = messagesToDownload.iterator();
		if (iter.hasNext()) {
			MessageReference msg = iter.next();
			iter.remove();
			return msg;
		} else {
			return null;
		}
	}
}
