package jfms.fms;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Request {
	private final Type type;
	private final String date;
	private final int index;

	public enum Type {
		IDENTITY,
		MESSAGE,
		MESSAGE_LIST,
		TRUST_LIST
	}

	public static Request getNextRequest(int identityId, Type type,
			LocalDate date, int maxIndex) {
		Store store = FmsManager.getInstance().getStore();

		Request lastRequest = store.getLastRequestFromHistory(
				identityId, type);
		if (lastRequest != null) {
			int compVal = date.compareTo(lastRequest.getLocalDate());
			if (compVal < 0) {
				// date before last date
				return null;
			} else if (compVal == 0) {
				// date == last date
				if (lastRequest.getIndex() < maxIndex) {
					return new Request(type, date, lastRequest.getIndex()+1);
				} else {
					return null;
				}
			}
		}

		// date after last date or no last date available
		return new Request(type, date, 0);
	}

	public Request(Type type, LocalDate date, int index) {
		this.type = type;
		this.date = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
		this.index = index;
	}

	public Type getType() {
		return type;
	}

	public LocalDate getLocalDate() {
		return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
	}

	public int getIndex() {
		return index;
	}
};
