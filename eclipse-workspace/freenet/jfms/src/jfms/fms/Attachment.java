package jfms.fms;

public class Attachment {
	private String key;
	private int size;

	public Attachment() {
		key = null;
		size = -1;
	}

	public Attachment(String key, int size) {
		this.key = key;
		this.size = size;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
};
