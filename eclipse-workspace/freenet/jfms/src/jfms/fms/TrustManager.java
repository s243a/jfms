package jfms.fms;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jfms.config.Config;
import jfms.config.Constants;

public class TrustManager {
	private static final Logger LOG = Logger.getLogger(TrustManager.class.getName());

	private Map<Integer, Integer> peerTrustListTrust;
	private Map<Integer, Integer> peerMessageTrust;

	static public class TrustLevel {
		private int trustSum = 0;
		private int weightSum = 0;

		public void addTrust(int trust, int weight) {
			if (trust >= 0) {
				trustSum += trust * weight;
				weightSum += weight;
			}
		}

		public int getTrust() {
			if (weightSum == 0) {
				return -1;
			}
			return ((trustSum + weightSum/2)/weightSum);
		}
	}

	public TrustManager() {
		peerTrustListTrust = Collections.emptyMap();
		peerMessageTrust = Collections.emptyMap();
	}

	public void initialize() {
		calculateTrustListTrusts();
		calculateMessageTrusts();
	}

	public Integer getLocalMessageTrust(int identityId) {
		Store store = FmsManager.getInstance().getStore();
		int localIdentityId = Config.getInstance().getDefaultId();
		return store.getLocalMessageTrust(localIdentityId, identityId);
	}

	public Integer getLocalTrustListTrust(int identityId) {
		Store store = FmsManager.getInstance().getStore();
		int localIdentityId = Config.getInstance().getDefaultId();
		return store.getLocalTrustListTrust(localIdentityId, identityId);
	}


	public void updateLocalTrust(int identityId, Integer messageTrust,
			Integer trustListTrust) {
		jfms.fms.Store store = FmsManager.getInstance().getStore();
		int localIdentityId = Config.getInstance().getDefaultId();

		Trust trust = new Trust();
		if (messageTrust != null) {
			trust.setTrustListTrustLevel(messageTrust);
		}
		if (trustListTrust != null) {
			trust.setMessageTrustLevel(trustListTrust);
		}
		store.updateLocalTrust(localIdentityId, identityId, trust);
	}


	public Integer getPeerMessageTrust(int identityId) {
		return peerMessageTrust.get(identityId);
	}

	public Integer getPeerTrustListTrust(int identityId) {
		return peerTrustListTrust.get(identityId);
	}

	public List<Integer> getTrustListTrustedIds() {
		return peerTrustListTrust.entrySet().stream()
			.filter(t -> t.getValue() >= Constants.MIN_TRUSTLIST_TRUST)
			.map(t -> t.getKey())
			.collect(Collectors.toList());
	}

	public void calculateTrustListTrusts() {
		LOG.log(Level.FINEST, "Recalculating trustlist trusts");

		Store store = FmsManager.getInstance().getStore();

		Map<Integer, TrustLevel> calculatedTrusts = new HashMap<>();

		int localIdentityId = Config.getInstance().getDefaultId();
		Map<Integer,Integer> trusts = store
			.getLocalTrustListTrusts(localIdentityId, Constants.MIN_TRUSTLIST_TRUST);

		LOG.log(Level.FINEST, "Found {0} IDs with local trustlist trust",
				trusts.size());
		if (trusts.isEmpty()) {
			LOG.log(Level.INFO, "No IDs with local trustlist trust found, "
					+ "using seed identities");
			IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
			for (String ssk : store.getSeedIdentitySsks()) {
				Integer id = identityManager.getIdentityId(ssk);
				if (id != null) {
					trusts.put(id, 50);
				}
			}
		}

		for (Map.Entry<Integer,Integer> t : trusts.entrySet()) {
			TrustLevel trust = new TrustLevel();
			trust.addTrust(100, t.getValue());
			calculatedTrusts.put(t.getKey(), trust);
		}

		// Assume "Six degrees of separation" is true for FMS
		for (int i=0; i<6; i++ ) {
			Map<Integer, TrustLevel> newCalculatedTrusts =
				updateTrustLevels(calculatedTrusts);
			LOG.log(Level.FINEST, "Found {0} IDs with trustlist trust "
					+ "in iteration {1}", new Object[]{
					newCalculatedTrusts.size(), i+1});
			if (newCalculatedTrusts.size() <= calculatedTrusts.size()) {
				break;
			}
			calculatedTrusts = newCalculatedTrusts;
		}

		peerTrustListTrust = calculatedTrusts.entrySet()
			.stream()
			.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getTrust()));

		LOG.log(Level.FINEST, "Recalculating trustlist trusts finished");
	}

	public void calculateMessageTrusts() {
		Store store = FmsManager.getInstance().getStore();
		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();

		Map<Integer, TrustLevel> calculatedTrusts = new HashMap<>();

		for (Map.Entry<Integer, Integer> e : peerTrustListTrust.entrySet()) {
			int identityId = e.getKey();
			int trustListTrust = e.getValue();
			List<Trust> trustList = store.getTrustList(identityId);

			for (Trust t : trustList) {
				if (t.getMessageTrustLevel() > 0) {
					Integer trustedIdentityId =
						identityManager.getIdentityId(t.getIdentity());
					if (trustedIdentityId == null) {
						LOG.log(Level.FINEST, "skipping unknown ID {0} for "
								+ "message trust calculation", t.getIdentity());
						continue;
					}

					TrustLevel tl = calculatedTrusts.get(trustedIdentityId);
					if (tl == null) {
						tl = new TrustLevel();
						calculatedTrusts.put(trustedIdentityId, tl);
					}

					tl.addTrust(t.getMessageTrustLevel(), trustListTrust);
				}
			}
		}

		peerMessageTrust = calculatedTrusts.entrySet()
			.stream()
			.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getTrust()));
	}

	public List<Integer> getMessageTrustedIds() {
		final Set<Integer> trustedIds;

		if (Config.getInstance().getDownloadMsgListsWithoutTrust()) {
			// 1a. initialize with all known identities
			// This will include identites without peer or local message trust
			// in the final set
			final IdentityManager identityManager =
				FmsManager.getInstance().getIdentityManager();
			trustedIds =
				new HashSet<>(identityManager.getIdentities().keySet());

			// 2a. Remove identities below peer message trust limit
			peerMessageTrust.entrySet().stream()
				.filter(e -> e.getValue() < Constants.MIN_PEER_MESSSAGE_TRUST)
				.forEach(e -> trustedIds.remove(e.getKey()));
		} else {
			// 1b. identities are not included by default

			// 2b. Add identities above peer message trust limit
			trustedIds = peerMessageTrust.entrySet().stream()
				.filter(e -> e.getValue() >= Constants.MIN_PEER_MESSSAGE_TRUST)
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
		}

		// 3. Override peer message trust by local message trust
		Store store = FmsManager.getInstance().getStore();
		final int localIdentityId = Config.getInstance().getDefaultId();

		store.getLocalMessageTrusts(localIdentityId).entrySet().stream()
			.forEach(e -> {
				if (e.getValue() < Constants.MIN_PEER_MESSSAGE_TRUST) {
					trustedIds.remove(e.getKey());
				} else {
					trustedIds.add(e.getKey());
				}
			});

		return Arrays.asList(trustedIds.toArray(new Integer[trustedIds.size()]));
	}

	public Map<Integer, TrustLevel> updateTrustLevels(Map<Integer, TrustLevel> trustLevels) {
		Store store = FmsManager.getInstance().getStore();
		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
		Map<Integer, TrustLevel> newTrusts = new HashMap<>();

		for (Map.Entry<Integer,TrustLevel> e : trustLevels.entrySet()) {
			int identityId = e.getKey();
			TrustLevel trust = e.getValue();

			if (trust.getTrust() < Constants.MIN_TRUSTLIST_TRUST) {
				// LOG.log(Level.FINEST, "skipping ID " + identityId);
				continue;
			}

			// LOG.log(Level.FINEST, "processing ID " + identityId);

			List<Trust> trustList = store.getTrustList(identityId);
			// LOG.log(Level.FINEST, "  found " + trustList.size() + " trustlist entries");

			for (Trust t : trustList) {
				if (t.getTrustListTrustLevel() > 0) {
					// LOG.log(Level.FINEST, "  ID " + identityId + " trusts " + t.getIdentity());
					Integer trustedIdentityId =
						identityManager.getIdentityId(t.getIdentity());
					if (trustedIdentityId == null) {
						LOG.log(Level.FINEST, "  skipping unknown ID {0} for "
								+ "trust list trust calculation", t.getIdentity());
						continue;
					}

					TrustLevel tl = newTrusts.get(trustedIdentityId);
					if (tl == null) {
						tl = new TrustLevel();
						newTrusts.put(trustedIdentityId, tl);
					}
					tl.addTrust(t.getTrustListTrustLevel(), trust.getTrust());
				}
			}
		}

		return newTrusts;
	}
};
