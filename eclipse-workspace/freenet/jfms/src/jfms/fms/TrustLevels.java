package jfms.fms;

public class TrustLevels {
	private int messageTrust = 0;
	private int messageTrustWeight = 0;
	private int trustListTrust = 0;
	private int trustListTrustWeight = 0;

	public void addPeerTrust(Trust trust, int weight) {
		if (trust.getMessageTrustLevel() >= 0) {
			messageTrust += trust.getMessageTrustLevel() * weight;
			messageTrustWeight += weight;
		}

		if (trust.getTrustListTrustLevel() >= 0) {
			trustListTrust += trust.getTrustListTrustLevel() * weight;
			trustListTrustWeight += weight;
		}
	}

	public int getMessageTrust() {
		if (messageTrustWeight == 0) {
			return -1;
		}

		return ((messageTrust + messageTrustWeight/2) /messageTrustWeight);
	}

	public int getTrustListTrust() {
		if (trustListTrustWeight == 0) {
			return -1;
		}

		return ((trustListTrust + trustListTrustWeight/2)/trustListTrustWeight);
	}
}
