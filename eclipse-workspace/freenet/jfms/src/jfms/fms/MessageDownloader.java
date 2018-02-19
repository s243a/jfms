package jfms.fms;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.config.Constants;
import jfms.fcp.FcpClient;
import jfms.fcp.FcpException;
import jfms.fcp.FcpListener;
import jfms.fcp.FcpRequest;
import jfms.util.RequestThread;

public class MessageDownloader extends RequestThread implements FcpListener{
	private static final Logger LOG = Logger.getLogger(MessageDownloader.class.getName());

	private final AtomicInteger nextFcpIdentifier = new AtomicInteger();

	private final Map<String, FcpRequest> fcpRequests = new ConcurrentHashMap<>();
	private final FcpClient fcpClient;

	private int failedCount;
	private int successfulCount;
	private int totalCount;
	private String countLabel;
	private boolean bootstrap = false;
	private final RequestCache requestCache = new RequestCache();
	private LocalDate today;

	public static FcpRequest createTrustListRequest(String fcpId,
			int identityId, String ssk, LocalDate date) {
		Request nextRequest = Request.getNextRequest(identityId,
				Request.Type.TRUST_LIST, date, Constants.MAX_INDEX);
		if (nextRequest == null) {
			return null;
		}

		return new TrustListRequest(fcpId, identityId,
				ssk, nextRequest.getLocalDate(), nextRequest.getIndex());
	}

	public static FcpRequest createMessageListRequest(String fcpId,
			int identityId, String ssk, LocalDate date,
			MessageReferenceList messagesToDownload) {
		Request nextRequest = Request.getNextRequest(identityId,
				Request.Type.MESSAGE_LIST, date, Constants.MAX_INDEX);
		if (nextRequest == null) {
			return null;
		}

		return new MessageListRequest(fcpId, identityId, ssk,
				nextRequest.getLocalDate(), nextRequest.getIndex(),
				messagesToDownload);
	}


	public MessageDownloader(FcpClient fcpClient) {
		this.fcpClient = fcpClient;
	}

	@Override
	public Integer call() {
		LOG.log(Level.INFO, "Starting FMS download thread with ID {0}",
				Thread.currentThread().getId());
		updateTitle("Idle");
		updateMessage("No requests pending");
		updateProgress(0.0, 1.0);

		try {
			Thread.sleep(Constants.STARTUP_IDLE_TIME * 1000);

			while (true) {
				if (FmsManager.getInstance().isOffline()) {
					Thread.sleep(Constants.ONLINE_CHECK_INTERVAL * 1000);
					continue;
				}

				requestCache.cleanup();

				today = LocalDate.now(ZoneOffset.UTC);
				LocalDate date = today;

				try {
					bootstrap = false;
					for (int i=0; i<Constants.MAX_IDENTITY_AGE; i++) {
						if (!bootstrap) {
							downloadMessagesForDay(today);
						}
						date = date.minusDays(1);
						downloadMessagesForDay(date);
					}
				} catch (FcpException e) {
					LOG.log(Level.WARNING, "Exception in download thread: " + e.getMessage(), e);
					fcpClient.close();
				}

				updateTitle("Idle");
				updateMessage("No requests pending");
				updateProgress(0.0, 1.0);

				if (!bootstrap) {
					Thread.sleep(Constants.DOWNLOAD_IDLE_TIME * 1000);
				}
			}
		} catch (InterruptedException e) {
			LOG.log(Level.FINE, "download thread interrupted");
		} catch (Exception e) {
			LOG.log(Level.WARNING, "exception in download thread", e);
		} finally {
			fcpClient.close();
		}

		LOG.log(Level.INFO, "Finished FMS download thread");
		updateTitle("Cancelled");

		return 0;
	}

	public String getNextFcpId() {
		StringBuilder str = new StringBuilder("request-");
		str.append(nextFcpIdentifier.getAndIncrement());

		return str.toString();
	}

	public void updateMessageProgress() {
		final int messageCount = successfulCount + failedCount;

		StringBuilder msg = new StringBuilder("Downloaded ");
		msg.append(messageCount);
		msg.append('/');
		msg.append(totalCount);
		msg.append(' ');
		msg.append(countLabel);
		msg.append(" (");
		msg.append(successfulCount);
		msg.append(" OK ");
		msg.append(failedCount);
		msg.append(" Failed)");
		updateMessage(msg.toString());

		updateProgress(messageCount, totalCount);
	}

	@Override
	public void fatalError(String message) {
		// TODO stop
	}

	@Override
	public void error(String fcpIdentifier, int code) {
		LOG.log(Level.FINE, "Request [{0}] failed", fcpIdentifier);

		try {
			FcpRequest fcpRequest = fcpRequests.remove(fcpIdentifier);
			if (fcpRequest != null) {
				fcpRequest.error();
				final int ttl = fcpRequest.getTtl();
				if (ttl > 0) {
					requestCache.addNegativeCacheEntry(fcpRequest.getKey(),
							Duration.ofMinutes(fcpRequest.getTtl()));
				}
			} else {
				LOG.log(Level.WARNING, "got FCP response for unknown ID: {0}", fcpIdentifier);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Failed to handle FCP response", e);
		}



		requestDone();

		failedCount++;
		updateMessageProgress();
	}

	@Override
	public void finished(String fcpIdentifier, byte[] data) {
		LOG.log(Level.FINEST, "request finished: ID {0} in Thread {1}", new Object[]{fcpIdentifier, Thread.currentThread().getId()});
		boolean lastInChain = true;
		try {
			FcpRequest fcpRequest = fcpRequests.remove(fcpIdentifier);
			if (fcpRequest != null) {
				fcpRequest.finished(data);

				if (fcpRequest instanceof IdentityRequest) {
					IdentityRequest idRequest = (IdentityRequest)fcpRequest;
					FcpRequest chainedRequest = idRequest.getChainedRequest();
					if (chainedRequest != null) {
						lastInChain = false;
						LOG.log(Level.FINEST, "Got chained request!");
						chainedRequest.setTtl(fcpRequest.getTtl());
						queueFcpRequest(chainedRequest);
					}
				}
			} else {
				LOG.log(Level.WARNING, "got FCP response for unknown ID: {0}", fcpIdentifier);
			}
		} catch (FcpException e) {
			LOG.log(Level.WARNING, "Failed to handle FCP response", e);
		}

		requestDone();

		if (lastInChain) {
			successfulCount++;
			updateMessageProgress();
		}
	}

	@Override
	public void redirect(String fcpIdentifier, String redirectURI) {
		LOG.log(Level.FINEST, "request redirected: ID {0} in Thread {1}", new Object[]{fcpIdentifier, Thread.currentThread().getId()});
		try {
			FcpRequest fcpRequest = fcpRequests.get(fcpIdentifier);
			if (fcpRequest != null) {
				if (fcpRequest.redirect(redirectURI)) {
					queueFcpRequest(fcpRequest);
				}
			} else {
				LOG.log(Level.WARNING, "got FCP response for unknown ID: {0}", fcpIdentifier);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Failed to handle FCP response", e);
		}

		requestDone();
	}

	@Override
	public void putSuccessful(String fcpIdentifier, String key) {
		LOG.log(Level.WARNING, "unexpected putSuccessful");
	}

	private void queueFcpRequest(FcpRequest fcpRequest) throws FcpException {
		final String key = fcpRequest.getKey();

		final String id = fcpRequest.getId();
		fcpRequests.put(id, fcpRequest);
		fcpClient.requestKey(id, key, this);

		addRequest();
	}

	private void downloadMessagesForDay(LocalDate date) throws InterruptedException, FcpException {
		LOG.log(Level.FINEST, "Starting downloading for {0}", date);

		TrustManager trustManager = FmsManager.getInstance().getTrustManager();
		int newCount = trustManager.getTrustListTrustedIds().size();
		if (newCount == 0) {
			LOG.log(Level.WARNING, "No trusted IDs found");
			return;
		}

		// TODO alternative: download seed IDs for X days back
		// maybe just don't store messagelist history during bootstrap
		// only try idx 0 during bootstrap?
		if (newCount <= 10) {
			LOG.log(Level.INFO, "Entering bootstrap mode");
			bootstrap = true;
		}

		int ratio = 0;
		do {
			int oldCount = newCount;
			downloadTrustLists(date);
			newCount = trustManager.getTrustListTrustedIds().size();

			if (newCount == oldCount) {
				LOG.log(Level.FINEST, "Number of trustlist trusted peers unchanged");
				break;
			}

			if (oldCount != 0) {
				ratio = (newCount*100) / oldCount;
			} else {
				ratio = 100;
			}

			LOG.log(Level.FINEST, "Number of trustlist trusted peers changed "
					+ "from: {0} to {1} ({2}%)", new Object[]{
					oldCount, newCount, ratio});
			if (!bootstrap && ratio >= Constants.BOOTSTRAP_RATIO) {
				LOG.log(Level.INFO, "Entering bootstrap mode");
				bootstrap = true;
			}
		} while (ratio >= Constants.BOOTSTRAP_RATIO);

		if (bootstrap) {
			LOG.log(Level.FINEST, "In bootstrap mode, skipping message download");
		} else {
			MessageReferenceList messagesToDownload = downloadMessageLists(date);
			messagesToDownload.cleanup();
			downloadMessages(messagesToDownload);
		}

		LOG.log(Level.FINEST, "Finished downloading for {0}", date);
	}


	private void downloadRequests(String label, LocalDate date,
			List<FcpRequest> requests)
		throws FcpException, InterruptedException {

		countLabel = label;
		successfulCount = 0;
		failedCount = 0;
		totalCount = requests.size();

		StringBuilder str = new StringBuilder();
		if (bootstrap) {
			str.append("Bootstrap Mode: ");
		}
		str.append("Downloading ");
		str.append(label);
		if (date != null) {
			str.append(" (");
			str.append(date);
			str.append(')');
		}
		updateTitle(str.toString());
		updateMessageProgress();

		for (FcpRequest request : requests) {
			waitUntilReady(Constants.MAX_FCP_REQUESTS);
			request.setId(getNextFcpId());
			queueFcpRequest(request);
		}
	}

	private void downloadTrustLists(LocalDate date) throws InterruptedException, FcpException {
		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
		TrustManager trustManager = FmsManager.getInstance().getTrustManager();

		List<Integer> trustlistTrustedIds = trustManager.getTrustListTrustedIds();

		int recentlyFailedCount = 0;
		int requestIdCount = 0;
		List<FcpRequest> requests = new ArrayList<>();

		for (int identityId : trustlistTrustedIds) {
			final Identity identity = identityManager.getIdentity(identityId);
			final String ssk = identity.getSsk();

			Request nextRequest = Request.getNextRequest(identityId,
					Request.Type.IDENTITY, date, 0);
			if (nextRequest == null) {
				// identity is up-to-date; request trust list directly
				if (!identity.getPublishTrustList()) {
					LOG.log(Level.FINEST, "Identity {0} does not publish a trust list", identityId);
					continue;
				}

				FcpRequest trustListRequest = createTrustListRequest(
					null, identityId, ssk, date);

				if (trustListRequest != null) {
					// trust list outdated; request next version
					if (requestCache.isPresent(trustListRequest.getKey())) {
						recentlyFailedCount++;
						continue;
					}

					if (date.equals(today)) {
						trustListRequest.setTtl(Constants.TTL_TODAY);
					} else {
						trustListRequest.setTtl(Constants.TTL_OLD);
					}

					requests.add(trustListRequest);
				}

				continue;
			}

			// identity is outdated; create identity request with chained
			// trust list request on success
			FcpRequest identityRequest = new IdentityRequest(null,
					identityId, ssk,
					nextRequest.getLocalDate(), nextRequest.getIndex());
			if (requestCache.isPresent(identityRequest.getKey())) {
				recentlyFailedCount++;
				continue;
			}

			if (date.equals(today)) {
				identityRequest.setTtl(Constants.TTL_TODAY);
			} else {
				identityRequest.setTtl(Constants.TTL_OLD);
			}
			requests.add(identityRequest);
			requestIdCount++;
		}

		LOG.log(Level.FINEST, "Requesting {0} trust lists "
			+ "({1} identity updates required)", new Object[]{
			requests.size(), requestIdCount});
		LOG.log(Level.FINEST, "Skipping {0} recently failed keys",
				recentlyFailedCount);

		if (requests.isEmpty()) {
			return;
		}

		downloadRequests("trust lists", date, requests);

		LOG.log(Level.FINE, "Waiting for trust lists to finish");
		waitUntilReady(1);
		LOG.log(Level.INFO, "TrustList download finished");

		if (successfulCount > 0) {
			trustManager.initialize();
		}
	}

	private MessageReferenceList downloadMessageLists(LocalDate date) throws InterruptedException, FcpException {
		TrustManager trustManager = FmsManager.getInstance().getTrustManager();
		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();

		List<Integer> trustedIds = trustManager.getMessageTrustedIds();

		int recentlyFailedCount = 0;
		int requestIdCount = 0;
		List<FcpRequest> requests = new ArrayList<>();
		MessageReferenceList messagesToDownload = new MessageReferenceList();

		for (int identityId : trustedIds) {
			final String ssk = identityManager.getSsk(identityId);

			Request nextRequest = Request.getNextRequest(identityId,
					Request.Type.IDENTITY, date, 0);
			if (nextRequest == null) {
				// identity is up-to-date; request message list directly
				FcpRequest messageListRequest = createMessageListRequest(
					null, identityId, ssk, date, messagesToDownload);

				if (messageListRequest != null) {
					// message list outdated; request next version
					if (requestCache.isPresent(messageListRequest.getKey())) {
						recentlyFailedCount++;
						continue;
					}

					if (date.equals(today)) {
						messageListRequest.setTtl(Constants.TTL_TODAY);
					} else {
						messageListRequest.setTtl(Constants.TTL_OLD);
					}

					requests.add(messageListRequest);
				}

				continue;
			}

			// identity is outdated; create identity request with chained
			// message list request on success
			FcpRequest identityRequest = new IdentityRequest(null,
					identityId, ssk,
					nextRequest.getLocalDate(), nextRequest.getIndex(),
					messagesToDownload);
			if (requestCache.isPresent(identityRequest.getKey())) {
				recentlyFailedCount++;
				continue;
			}

			if (date.equals(today)) {
				identityRequest.setTtl(Constants.TTL_TODAY);
			} else {
				identityRequest.setTtl(Constants.TTL_OLD);
			}
			requests.add(identityRequest);
			requestIdCount++;
		}

		LOG.log(Level.FINEST, "Requesting {0} message lists "
			+ "({1} identity updates required)", new Object[]{
			requests.size(), requestIdCount});
		LOG.log(Level.FINEST, "Skipped {0} recently failed keys",
				recentlyFailedCount);

		if (requests.isEmpty()) {
			return messagesToDownload;
		}

		downloadRequests("message lists", date, requests);

		LOG.log(Level.FINEST, "Waiting for message lists to finish");
		waitUntilReady(1);

		return messagesToDownload;
	}

	private void downloadMessages(MessageReferenceList messagesToDownload) throws InterruptedException, FcpException {
		int recentlyFailedCount = 0;

		if (!messagesToDownload.isEmpty()) {
			// TODO validate MessageID
			List<FcpRequest> requests = new ArrayList<>();
			MessageReference msg;
			while ((msg = messagesToDownload.remove()) != null) {
				FcpRequest messageRequest = new MessageRequest(null, msg);
				if (requestCache.isPresent(messageRequest.getKey())) {
					recentlyFailedCount++;
					continue;
				}

				messageRequest.setTtl(Constants.TTL_OLD);
				requests.add(messageRequest);
			}

			LOG.log(Level.FINEST, "Requesting {0} messages", requests.size());
			LOG.log(Level.FINEST, "Skipping {0} recently failed keys",
					recentlyFailedCount);

			downloadRequests("messages", null, requests);

			LOG.log(Level.FINEST, "Waiting for messages to finish");
			waitUntilReady(1);
		}

		Store store = FmsManager.getInstance().getStore();
		for (MessageListReference ml : messagesToDownload.getMessageListsFinished()) {
			// TODO optimize, i.e., batch request
			store.updateRequestHistory(ml.getIdentityId(),
					Request.Type.MESSAGE_LIST,
					ml.getDate(),
					ml.getIndex());
		}

		Thread.sleep(5000);
	}
}
