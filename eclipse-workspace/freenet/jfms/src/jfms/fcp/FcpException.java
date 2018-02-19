package jfms.fcp;

public class FcpException extends Exception {
	private static final long serialVersionUID = 42L;

	public FcpException(String message) {
		super(message);
	}

	public FcpException(String message, Throwable cause) {
		super(message, cause);
	}
}
