package jfms.fms;

import java.util.Objects;

public class Trust {
	private String identity;
	private int messageTrustLevel = -1;
	private int trustListTrustLevel = -1;
	private String messageTrustComment;
	private String trustListTrustComment;

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof Trust)) {
			return false;
		}

		final Trust t = (Trust)o;

		return
			Objects.equals(identity, t.identity) &&
			messageTrustLevel == t.messageTrustLevel &&
			trustListTrustLevel != t.trustListTrustLevel &&
			Objects.equals(messageTrustComment, t.messageTrustComment) &&
			Objects.equals(trustListTrustComment, t.trustListTrustComment);
	}

	@Override
	public int hashCode() {
		return
			13 * Objects.hashCode(identity) +
			17 * messageTrustLevel +
			19 * trustListTrustLevel +
			23 * Objects.hashCode(messageTrustComment) +
			29 * Objects.hashCode(trustListTrustComment);
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public void setMessageTrustLevel(int messageTrustLevel) {
		this.messageTrustLevel = messageTrustLevel;
	}

	public int getMessageTrustLevel() {
		return messageTrustLevel;
	}

	public void setTrustListTrustLevel(int trustListTrustLevel) {
		this.trustListTrustLevel = trustListTrustLevel;
	}

	public int getTrustListTrustLevel() {
		return trustListTrustLevel;
	}

	public void setMessageTrustComment(String messageTrustComment) {
		this.messageTrustComment = messageTrustComment;
	}

	public String getMessageTrustComment() {
		return messageTrustComment;
	}

	public void setTrustListTrustComment(String trustListTrustComment) {
		this.trustListTrustComment = trustListTrustComment;
	}

	public String getTrustListTrustComment() {
		return trustListTrustComment;
	}
}
