package jfms.fcp;

public abstract class FcpRequest {
	private String id;
	private String key;
	private int ttl = -1;

	public FcpRequest(String id, String key) {
		this.id = id;
		this.key = key;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public int getTtl() {
		return ttl;
	}

	public abstract void finished(byte[] data);

	public boolean redirect(String redirectURI) {
		return false;
	}

	public void error() {
	}
};
