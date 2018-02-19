package jfms.fms;

import java.time.LocalDate;

public class MessageListReference {
	private final int identityId;
	private final LocalDate date;
	private int index = -1;

	public MessageListReference(int identityId, LocalDate date, int index) {
		this.identityId = identityId;
		this.date = date;
		this.index = index;
	}

	public int getIdentityId() {
		return identityId;
	}

	public LocalDate getDate() {
		return date;
	}

	public int getIndex() {
		return index;
	}
};
