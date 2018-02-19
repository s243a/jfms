package jfms.fms;

import java.util.Objects;

public class LocalIdentity extends Identity {
	private String privateSsk;
	private boolean isActive;

	public String getPrivateSsk() {
		return privateSsk;
	}

	public void setPrivateSsk(String privateSsk) {
		this.privateSsk = privateSsk;
	}

	public boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!super.equals(o)) {
			return false;
		}

		if (!(o instanceof LocalIdentity)) {
			return false;
		}

		final LocalIdentity id = (LocalIdentity)o;

		return Objects.equals(privateSsk, id.privateSsk);
	}

	@Override
	public int hashCode() {
		return
			super.hashCode() +
			41 * Objects.hashCode(privateSsk);
	}

}
