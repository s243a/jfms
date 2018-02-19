package jfms.fms;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.fcp.FcpRequest;
import jfms.fms.xml.IdentityParser;

public class IdentityRequest extends FcpRequest {
	private static final Logger LOG = Logger.getLogger(IdentityRequest.class.getName());
	private static final IdentityParser identityParser = new IdentityParser();

	private final int identityId;
	private final String ssk;
	private FcpRequest chainedRequest;
	private final SuccessAction successAction;
	private final LocalDate date;
	private final int index;
	private final MessageReferenceList messagesToDownload;

	public enum SuccessAction {
		NONE,
		REQUEST_TRUSTLIST,
		REQUEST_MESSAGELIST
	}

	public IdentityRequest(String id, int identityId, String ssk, LocalDate date,
			int index) {
		super(id, Identity.getIdentityKey(ssk, date, index));
		this.identityId = identityId;
		this.index = index;
		this.ssk = ssk;
		this.date = date;
		this.successAction = SuccessAction.REQUEST_TRUSTLIST;
		this.messagesToDownload = null;
	}

	public IdentityRequest(String id, int identityId, String ssk, LocalDate date,
			int index, MessageReferenceList messagesToDownload) {
		super(id, Identity.getIdentityKey(ssk, date, index));
		this.identityId = identityId;
		this.ssk = ssk;
		this.date = date;
		this.index = index;
		this.successAction = SuccessAction.REQUEST_MESSAGELIST;
		this.messagesToDownload = messagesToDownload;
	}

	private String getNextId(String id) {
		String mainId;
		int subId;
		int dotIndex = id.indexOf('.');
		if (dotIndex >= 0) {
			mainId = id.substring(0, dotIndex);
			subId = Integer.parseInt(id.substring(dotIndex+1));
		} else {
			mainId = id;
			subId = 0;
		}

		StringBuilder str = new StringBuilder(mainId);
		str.append('.');
		str.append(++subId);

		return str.toString();
	}

	@Override
	public void finished(byte[] data) {
		Store store = FmsManager.getInstance().getStore();
		Identity identity = identityParser.parse(new ByteArrayInputStream(data));
		identity.setSsk(ssk);
		LOG.log(Level.FINE, "retrieved Identity of {0}",
				identity.getFullName());

		IdentityManager identityManager =
			FmsManager.getInstance().getIdentityManager();
		identityManager.updateIdentity(identityId, identity);

		store.updateRequestHistory(identityId, Request.Type.IDENTITY,
				date, index);

		switch (successAction) {
		case NONE:
			break;
		case REQUEST_TRUSTLIST:
			if (identity.getPublishTrustList()) {
				chainedRequest = MessageDownloader.createTrustListRequest(
						getNextId(getId()),
						identityId, ssk, date);
			} else {
				LOG.log(Level.FINEST,
						"Identity {0} does not publish a trust list",
						identityId);
			}
			break;
		case REQUEST_MESSAGELIST:
			chainedRequest = MessageDownloader.createMessageListRequest(
				getNextId(getId()),
				identityId, ssk, date, messagesToDownload);
			break;
		}
	}

	@Override
	public boolean redirect(String redirectURI) {
		LOG.log(Level.WARNING, "got unexpected redirect");

		return false;
	}

	@Override
	public void error() {
		LOG.log(Level.FINEST, "failed to retrieve {0}", getKey());
	}

	public FcpRequest getChainedRequest() {
		return chainedRequest;
	}
};
