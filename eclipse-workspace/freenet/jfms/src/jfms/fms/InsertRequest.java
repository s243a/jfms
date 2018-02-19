package jfms.fms;

import java.time.LocalDate;

public class InsertRequest extends Request {
	private final int localIdentityId;

	public InsertRequest(Type type, int localIdentityId, LocalDate date,
			int index) {
		super(type, date, index);
		this.localIdentityId = localIdentityId;
	}

	public int getLocalIdentityId() {
		return localIdentityId;
	}
}
