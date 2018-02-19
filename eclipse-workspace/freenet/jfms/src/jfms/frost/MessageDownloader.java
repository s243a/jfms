package jfms.frost;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.fcp.FcpClient;
import jfms.fcp.FcpException;
import jfms.fcp.FcpListener;
import jfms.fcp.FcpRequest;
import jfms.util.RequestThread;

public class MessageDownloader extends RequestThread implements FcpListener{
	private static final Logger LOG = Logger.getLogger(MessageDownloader.class.getName());
	private static final DateTimeFormatter dateFormatter =
			DateTimeFormatter.ofPattern("yyyy.M.d");

	private final Map<String, FcpRequest> fcpRequests = new HashMap<>();
	private long fcpIdentifier = 0;
	private int downloadedMessages = 0;
	private final MessageParser frostMessageParser = new MessageParser();
	private FcpClient fcpClient;

	static class MessageAdder implements Runnable {
		private final String boardName;
		private final Message msg;

		public MessageAdder(String boardName, Message msg) {
			this.msg = msg;
			this.boardName = boardName;
		}

		@Override
		public void run() {
			final BoardManager boardManager = FrostManager.getInstance().getBoardManager();
			Board board = boardManager.getBoard(boardName);
			if (board == null) {
				LOG.log(Level.FINE, "unknown board");
				return;
			}
			board.addMessage(msg);
		}
	}

	MessageDownloader() {
	}


	public void queueFcpRequest(FcpRequest fcpRequest) throws FcpException {
		final String id = fcpRequest.getId();
		fcpRequests.put(id, fcpRequest);
		fcpClient.requestKey(id, fcpRequest.getKey(), this);

		addRequest();
	}

	@Override
	public Integer call() {
		LOG.log(Level.INFO, "Starting frost download thread");
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		final Config config = Config.getInstance();
		fcpClient = new FcpClient("jfms-frost",
			config.getFcpHost(), config.getFcpPort());
		try {
			for (int i=0; i<5; i++) {
				downloadMessagesForDate(date);
				date = date.minusDays(1);
			}
		} catch (FcpException e) {
			LOG.log(Level.WARNING, "FcpException in download thread", e);
		} catch (InterruptedException e) {
			LOG.log(Level.WARNING, "Exception in download thread", e);
		} finally {
			fcpClient.close();
		}
		LOG.log(Level.INFO, "Finished frost download thread");
		return downloadedMessages;
	}

	@Override
	public void fatalError(String message) {
		// TODO
	}

	@Override
	public void error(String identifier, int code) {
		FcpRequest requestInfo = fcpRequests.get(identifier);
		requestDone();
		if (requestInfo == null) {
			LOG.log(Level.WARNING, "unknown request ID {0}", identifier);
			return;
		}

		LOG.log(Level.FINE, "request failed: ID {0}", identifier);
		fcpRequests.remove(identifier);
	}

	@Override
	public void finished(String identifier, byte[] data) {
		final Store store = FrostManager.getInstance().getStore();
		try {
			FcpRequest fcpRequest = fcpRequests.remove(identifier);
			if (fcpRequest == null) {
				requestDone();
				LOG.log(Level.WARNING, "got FCP response for unknown ID: {0}",
						identifier);
				return;
			}
			LOG.log(Level.INFO, "got frost FCP response");
			fcpRequest.finished(data);

			// TODO move into handler
			if (fcpRequest instanceof MessageRequest) {
				MessageRequest req = (MessageRequest)fcpRequest;
				RequestInfo requestInfo = req.getRequestInfo();
				String dataTmp = new String(data);
				Message msg = parseXmlMessage(dataTmp);
				msg.setIndex(requestInfo.getIndex());
				msg.setSlotDate(requestInfo.getDate());

				store.store(requestInfo, dataTmp);
				Platform.runLater(new MessageAdder(requestInfo.getBoardName(), msg));

				requestInfo.incrementIndex();
				fcpIdentifier++;
				FcpRequest nextFcpRequest = new MessageRequest(Long.toString(fcpIdentifier), requestInfo);
				queueFcpRequest(nextFcpRequest);

				requestDone();
			}
		} catch (NumberFormatException e) {
			LOG.log(Level.WARNING, "failed to parse request ID {0}", identifier);
		} catch (FcpException e) {
			LOG.log(Level.WARNING, "failed to handle FCP response", e);
		}
	}

	@Override
	public void redirect(String fcpIdentifier, String redirectURI) {
		LOG.log(Level.WARNING, "ignoring redirect");
	}

	@Override
	public void putSuccessful(String fcpIdentifier, String key) {
		LOG.log(Level.WARNING, "unexpected putSuccessful");
	}

	private void downloadMessagesForDate(LocalDate date) throws InterruptedException, FcpException{
		LOG.log(Level.FINE, "Downloading messages for {0}", date);
		final BoardManager boardManager = FrostManager.getInstance().getBoardManager();
		final Store store = FrostManager.getInstance().getStore();

		for (Board board : boardManager.getBoardList()) {
			String slotDate = date.format(dateFormatter);
			RequestInfo requestInfo = new RequestInfo(
					board.getName(), slotDate, 0);

			while (store.exists(requestInfo)) {
				Message msg = parseXmlMessage(store.retrieve(requestInfo));
				msg.setIndex(requestInfo.getIndex());
				msg.setSlotDate(slotDate);
				LOG.log(Level.FINE, "key already in store{0}",
						requestInfo.getMessageKey());
				requestInfo.incrementIndex();
			}

			fcpIdentifier++;
			FcpRequest fcpRequest = new MessageRequest(Long.toString(fcpIdentifier), requestInfo);

			waitUntilReady(Constants.MAX_FCP_REQUESTS);
			queueFcpRequest(fcpRequest);
			downloadedMessages++;
		}

		LOG.log(Level.FINEST, "Waiting for requests to finish");
		waitUntilReady(1);
	}

	private Message parseXmlMessage(String xmlMessage) {
		return frostMessageParser.parse(new ByteArrayInputStream(xmlMessage.getBytes(StandardCharsets.UTF_8)));
	}
}
