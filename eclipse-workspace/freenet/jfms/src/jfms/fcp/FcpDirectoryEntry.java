package jfms.fcp;

public class FcpDirectoryEntry {
	private final String name;
	private final byte[] data;

	public FcpDirectoryEntry(String name, byte[] data) {
		this.name = name;
		this.data = data;
	}

	public String getName() {
		return name;
	}

	public byte[] getData() {
		return data;
	}
}
