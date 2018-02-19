package jfms.fcp;

import java.util.HashMap;
import java.util.Map;

public class FcpResponse {
	private final String name;
	private final Map<String, String> fields;
	private byte[] data;

	public FcpResponse(String name) {
		this.name = name;
		fields = new HashMap<>();
	}

	public String getName() {
		return name;
	}

	public String getField(String key) {
		return fields.get(key);
	}

	public void addField(String key, String value) {
		fields.put(key, value);
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
};
