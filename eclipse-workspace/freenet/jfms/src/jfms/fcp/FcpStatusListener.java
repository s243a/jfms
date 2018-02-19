package jfms.fcp;

public interface FcpStatusListener {
	void statusChanged(FcpClient.Status status);
}
