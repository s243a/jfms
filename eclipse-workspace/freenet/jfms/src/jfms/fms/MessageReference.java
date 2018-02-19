package jfms.fms;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class MessageReference {
	private int identityId;
	private String ssk;
	private LocalDate date;
	private int index = -1;
	private List<String> boards;

	public int getIdentityId() {
		return identityId;
	}

	public void setIdentityId(int identityId) {
		this.identityId = identityId;
	}

	public void setSsk(String ssk) {
		this.ssk = ssk;
	}

	public String getSsk() {
		return ssk;
	}

	public String getPublicKeyHash() {
		return ssk.substring(4, 47);
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public List<String> getBoards() {
		return boards;
	}

	public void setBoards(List<String> boards) {
		this.boards = boards;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o instanceof MessageReference) {
			MessageReference m = (MessageReference)o;
			return identityId == m.identityId &&
				Objects.equals(date, m.date) &&
				index == m.index &&
				Objects.equals(boards, m.boards);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return
			13 * identityId +
			17 * Objects.hashCode(date) +
			19 * index +
			23 * Objects.hashCode(boards);
	}

	public boolean isValid() {
		return identityId >= 0 && date != null && index > 0 && boards != null;
	}
};
