package jfms.fms;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.fcp.FcpRequest;
import jfms.fms.xml.TrustListParser;

public class TrustListRequest extends FcpRequest {
	private static final Logger LOG = Logger.getLogger(TrustListRequest.class.getName());
	private static final TrustListParser trustListParser = new TrustListParser();

	private final int identityId;
	private final LocalDate date;
	private final int index;

	public TrustListRequest(String id, int identityId, String ssk, LocalDate date, int index) {
		super(id, Identity.getTrustListKey(ssk, date, index));
		this.identityId = identityId;
		this.date = date;
		this.index = index;
	}

	@Override
	public void finished(byte[] data) {
		Store store = FmsManager.getInstance().getStore();

		List<Trust> trusts = trustListParser.parse(new ByteArrayInputStream(data));
		store.insertTrustList(identityId, trusts);

		// TODO set correct index, respect redirects, only newer
		store.updateRequestHistory(identityId, Request.Type.TRUST_LIST,
				date, index);
	}

	@Override
	public boolean redirect(String redirectURI) {
		LOG.log(Level.FINEST, "got unexpected redirect");

		return false;
	}
};
