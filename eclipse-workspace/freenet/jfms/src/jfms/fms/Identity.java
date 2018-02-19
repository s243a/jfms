package jfms.fms;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import jfms.config.Config;

public class Identity {
	private static final DateTimeFormatter dotDateFormatter =
		DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private String ssk;
	private String name;
	private String signature;
	private String avatar;
	private boolean singleUse = false;
	private boolean publishTrustList = false;
	private boolean publishBoardList = false;
	private int freesiteEdition = -1;

	public static String getIdentityKey(String ssk, LocalDate date, int index) {
		StringBuilder str = new StringBuilder(ssk);
		str.append(Config.getInstance().getMessageBase());
		str.append('|');
		str.append(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
		str.append("|Identity|");
		str.append(index);
		str.append(".xml");

		return str.toString();
	}

	public static String getMessageListKey(String ssk, LocalDate date, int index, boolean includeFilename) {
		StringBuilder str = new StringBuilder("USK");
		str.append(ssk.substring(3));
		str.append(Config.getInstance().getMessageBase());
		str.append('|');
		str.append(date.format(dotDateFormatter));
		str.append("|MessageList/");
		str.append(index);
		if (includeFilename) {
			str.append("/MessageList.xml");
		}

		return str.toString();
	}

	public static String getMessageKey(String ssk, LocalDate date, int index) {
		// we don't support the edition based SSK
		StringBuilder str = new StringBuilder(ssk);
		str.append(Config.getInstance().getMessageBase());
		str.append('|');
		str.append(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
		str.append("|Message|");
		str.append(index);
		str.append(".xml");

		return str.toString();
	}

	public static String getTrustListKey(String ssk, LocalDate date, int index) {
		StringBuilder str = new StringBuilder(ssk);
		str.append(Config.getInstance().getMessageBase());
		str.append('|');
		str.append(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
		str.append("|TrustList|");
		str.append(index);
		str.append(".xml");

		return str.toString();
	}

	public static String getPublicKeyHash(String ssk) {
		return ssk.substring(4, 47);
	}

	public Identity() {
	}

	public Identity(String ssk) {
		if (!Validator.isValidSsk(ssk)) {
			// TODO use less general exception
			throw new RuntimeException("SSK invalid: " + ssk);
		}
		this.ssk = ssk;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof Identity)) {
			return false;
		}

		final Identity id = (Identity)o;

		return
			Objects.equals(ssk, id.ssk) &&
			Objects.equals(name, id.name) &&
			Objects.equals(signature, id.signature) &&
			Objects.equals(avatar, id.avatar) &&
			singleUse == id.singleUse &&
			publishTrustList == id.publishTrustList &&
			publishBoardList == id.publishBoardList &&
			freesiteEdition == id.freesiteEdition;
	}

	@Override
	public int hashCode() {
	return
		13 * Objects.hashCode(ssk) +
		17 * Objects.hashCode(signature) +
		19 * Objects.hashCode(avatar) +
		23 * (singleUse ? 1 : 0) +
		29 * (publishTrustList ? 1 : 0) +
		31 * (publishBoardList ? 1 : 0) +
		37 * freesiteEdition;
	}

	public boolean isValid() {
		return ssk != null;
	}

	public String getFullName() {
		StringBuilder friendlyName = new StringBuilder();
		if (name != null) {
			friendlyName.append(name);
		}
		friendlyName.append('@');
		friendlyName.append(getPublicKeyHash());

		return friendlyName.toString();
	}

	public String getPublicKeyHash() {
		return ssk.substring(4, 47);
	}

	public String getSsk() {
		return ssk;
	}

	public void setSsk(String ssk) {
		if (!Validator.isValidSsk(ssk)) {
			// TODO use less general exception
			throw new RuntimeException("SSK invalid: " + ssk);
		}
		this.ssk = ssk;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public boolean getSingleUse() {
		return singleUse;
	}

	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	public boolean getPublishTrustList() {
		return publishTrustList;
	}

	public void setPublishTrustList(boolean publishTrustList) {
		this.publishTrustList = publishTrustList;
	}

	public boolean getPublishBoardList() {
		return publishBoardList;
	}

	public void setPublishBoardList(boolean publishBoardList) {
		this.publishBoardList = publishBoardList;
	}

	public int getFreesiteEdition() {
		return freesiteEdition;
	}

	public void setFreesiteEdition(int freesiteEdition) {
		this.freesiteEdition = freesiteEdition;
	}
}
