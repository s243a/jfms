package jfms.fms;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.config.Constants;
import jfms.fcp.FcpClient;
import jfms.fcp.FcpDirectoryEntry;
import jfms.fcp.FcpException;
import jfms.fcp.FcpListener;
import jfms.fms.xml.IdentityWriter;
import jfms.fms.xml.MessageListWriter;
import jfms.fms.xml.MessageParser;
import jfms.util.RequestThread;

public class InsertThread extends RequestThread implements FcpListener {
	public static final int FCP_ERROR_COLLISION = 9;
	private static final Logger LOG = Logger.getLogger(InsertThread.class.getName());

	private final FcpClient fcpClient;
	private final AtomicInteger nextFcpIdentifier = new AtomicInteger();
	private final Map<String, InsertRequest> requests = new ConcurrentHashMap<>();

	public InsertThread(FcpClient fcpClient) {
		this.fcpClient = fcpClient;
	}

	@Override
	public Integer call() {
		LOG.log(Level.FINEST, "Starting insert thread");
		Store store = FmsManager.getInstance().getStore();

		try {
			Thread.sleep(Constants.STARTUP_IDLE_TIME * 1000);

			while (true) {
				if (FmsManager.getInstance().isOffline()) {
					Thread.sleep(Constants.ONLINE_CHECK_INTERVAL * 1000);
					continue;
				}

				LocalDate date = LocalDate.now(ZoneOffset.UTC);

				Map<Integer, LocalIdentity> identities = store.retrieveLocalIdentities();
				for (Map.Entry<Integer, LocalIdentity> e : identities.entrySet()) {

					final int localIdentityId = e.getKey();
					final LocalIdentity localIdentity = e.getValue();

					LOG.log(Level.FINEST, "Processing local identity {0}: {1}",
							new Object[]{e.getKey(),
							localIdentity.getFullName()});
					if (!localIdentity.getIsActive()) {
						LOG.log(Level.FINEST, "identity ist not active");
						continue;
					}

					waitUntilReady(1);

					insertIdentities(localIdentityId, localIdentity, date);
					insertMessages(localIdentityId, localIdentity);
					insertMessageList(localIdentityId, localIdentity, date);
				}

				Thread.sleep(Constants.INSERT_IDLE_TIME * 1000);
			}
		} catch (InterruptedException e) {
			LOG.log(Level.FINE, "insert thread interrupted");
		} catch (FcpException e) {
			// TODO recover from errors, i.e. retry to connect
			LOG.log(Level.WARNING, "exception in insert thread", e);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "exception in insert thread", e);
		} finally {
			fcpClient.close();
		}

		LOG.log(Level.FINEST, "Finished insert thread");

		return 0;
	}

	@Override
	public void fatalError(String message) {
		// TODO stop
	}

	@Override
	public void error(String fcpIdentifier, int code) {
		requestDone();

		InsertRequest request = requests.remove(fcpIdentifier);
		if (request == null) {
			LOG.log(Level.WARNING, "got FCP response for unknown ID: {0}",
					fcpIdentifier);
			return;
		}

		Store store = FmsManager.getInstance().getStore();
		if (Request.Type.IDENTITY.equals(request.getType())) {
			// TODO check for fatal instead?
			if (code == FCP_ERROR_COLLISION) {
				// Collided with existing data
				// assume insert was successful and set inserted flag in DB
				LOG.log(Level.WARNING, "collison on identity insert");
				store.updateLocalIdentityInsert(request.getLocalIdentityId(),
						request.getLocalDate(),
						request.getIndex());
			}
		} else if (Request.Type.MESSAGE.equals(request.getType())) {
			if (code == FCP_ERROR_COLLISION) {
				// Collided with existing data
				// increment index and retry
				LOG.log(Level.WARNING, "collison on message insert, retrying with new index");
				store.updateLocalMessageIndex(request.getLocalIdentityId(),
						request.getLocalDate(),
						request.getIndex());
			}
		}
	}

	@Override
	public void finished(String fcpIdentifier, byte[] data) {
		requestDone();
	}

	@Override
	public void redirect(String fcpIdentifier, String redirectURI) {
		requestDone();

		requests.remove(fcpIdentifier);
	}

	@Override
	public void putSuccessful(String fcpIdentifier, String key) {
		requestDone();

		InsertRequest request = requests.remove(fcpIdentifier);
		if (request == null) {
			LOG.log(Level.WARNING, "got FCP response for unknown ID: {0}",
					fcpIdentifier);
			return;
		}

		Store store = FmsManager.getInstance().getStore();
		switch (request.getType()) {
		case IDENTITY:
			store.updateLocalIdentityInsert(request.getLocalIdentityId(),
					request.getLocalDate(),
					request.getIndex());
			break;
		case MESSAGE:
			store.setLocalMessageInserted(request.getLocalIdentityId(),
					request.getLocalDate(),
					request.getIndex(),
					true);

			LocalDate date = LocalDate.now(ZoneOffset.UTC);
			int index = store.getMessageListInsertIndex(
					request.getLocalIdentityId(), date, true);
			store.updateMessageListInsert(request.getLocalIdentityId(),
					date, index+1, false);
			break;
		case MESSAGE_LIST:
			store.updateMessageListInsert(request.getLocalIdentityId(),
					request.getLocalDate(), request.getIndex(), true);
			break;
		default:
			LOG.log(Level.WARNING, "unhandled type: {0}", request.getType());
		}
	}

	private void insertIdentities(int localIdentityId,
			LocalIdentity localIdentity, LocalDate date)
			throws InterruptedException, FcpException{
		Store store = FmsManager.getInstance().getStore();

		IdentityWriter identityWriter = new IdentityWriter();

		int currentIndex = store.getLocalIdentityInsertIndex(localIdentityId, date);
		// currently, only insert identity once per day
		// -> changes will be effective on the next day
		if (currentIndex == -1) {
			waitUntilReady(Constants.MAX_INSERTS);

			LOG.log(Level.FINEST, "Inserting identity");

			InsertRequest request = new InsertRequest(
					Request.Type.IDENTITY,
					localIdentityId, date, 0);
			final String key = Identity.getIdentityKey(
					localIdentity.getPrivateSsk(), date, 0);
			byte[] identityXml = identityWriter.writeXml(localIdentity);

			queueRequest(request, key, identityXml);
		} else {
			LOG.log(Level.FINEST, "Skipping identity insert");
		}
	}

	private void insertMessages(int localIdentityId,
			LocalIdentity localIdentity) throws InterruptedException, FcpException {
		Store store = FmsManager.getInstance().getStore();
		final List<MessageReference> localMessages =
			store.getLocalMessageList(localIdentityId, true, -1);

		LOG.log(Level.FINEST, "found {0} local messages for insert",
				localMessages.size());
		for (MessageReference m : localMessages) {
			waitUntilReady(Constants.MAX_INSERTS);

			InsertRequest request = new InsertRequest(
					Request.Type.MESSAGE,
					localIdentityId, m.getDate(), m.getIndex());

			final String key = Identity.getMessageKey(
					localIdentity.getPrivateSsk(), m.getDate(), m.getIndex());
			final String messageXml = store.getLocalMessage(localIdentityId,
					m.getDate(), m.getIndex());
			final byte[] data = messageXml.getBytes(StandardCharsets.UTF_8);

			queueRequest(request, key, data);
		}

		// wait for message inserts to finish before proceeding with
		// message list insert
		if (localMessages.size() > 0) {
			waitUntilReady(1);
		}
	}

	private void insertMessageList(int localIdentityId,
			LocalIdentity localIdentity, LocalDate date) throws InterruptedException, FcpException {
		Store store = FmsManager.getInstance().getStore();
		// check for a message list that has not been inserted yet
		int index = store.getMessageListInsertIndex(localIdentityId, date, true);
		if (index < 0) {
			// check if we have already inserted message list for today
			if (store.getMessageListInsertIndex(localIdentityId, date, false) == -1) {
				// insert with index 0
				index = 0;
			}
		}

		if (index < 0) {
			LOG.log(Level.FINEST, "Skipping message list insert");
			return;
		}

		final int maxMessages = Constants.MAX_MESSAGELIST_COUNT;

		// TODO FMS limits by age instead of count
		// XXX should we store boards to avoid parsing of XML files?
		final List<MessageReference> localMessages =
			store.getLocalMessageList(localIdentityId, false, Constants.MAX_LOCAL_MESSAGELIST_COUNT);
		LOG.log(Level.FINEST, "found {0} local messages", localMessages.size());
		for (MessageReference m : localMessages) {
			waitUntilReady(Constants.MAX_INSERTS);

			String xmlMessage = store.getLocalMessage(localIdentityId,
					m.getDate(), m.getIndex());

			MessageParser parser = new MessageParser();
			Message msg = parser.parse(new StringReader(xmlMessage));
			m.setBoards(msg.getBoards());
		}

		final int maxExternalMessages = maxMessages - localMessages.size();
		List<MessageReference> externalMessageList =
			store.getExternalMessageList(localIdentityId, maxExternalMessages);

		if (localMessages.size() + externalMessageList.size() == 0) {
			LOG.log(Level.FINE, "No messages found, skipping MessageList insert");
			return;
		}

		// XXX necessary to store index?
		MessageListWriter messageListWriter = new MessageListWriter();
		byte[] messageListXml = messageListWriter.writeXml(localMessages, externalMessageList);

		InsertRequest request = new InsertRequest(
				Request.Type.MESSAGE_LIST,
				localIdentityId, date, index);
		final String key = Identity.getMessageListKey(
				localIdentity.getPrivateSsk(), date, index, false);

		final String id = getNextFcpId();
		requests.put(id, request);
		fcpClient.insertDirectory(id, key,
				new FcpDirectoryEntry[] {
					new FcpDirectoryEntry("MessageList.xml", messageListXml)
				},
				"MessageList.xml", this);

		addRequest();
	}

	private String getNextFcpId() {
		StringBuilder str = new StringBuilder("insert-");
		str.append(nextFcpIdentifier.getAndIncrement());

		return str.toString();
	}

	private void queueRequest(InsertRequest request, String key, byte[] data) throws FcpException {
		final String id = getNextFcpId();
		requests.put(id, request);
		fcpClient.insertKey(id, key, data, this);

		addRequest();
	}
}
