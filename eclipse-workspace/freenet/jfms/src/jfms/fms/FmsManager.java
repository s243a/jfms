package jfms.fms;

import java.sql.SQLException;
import java.util.List;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.fcp.FcpClient;
import jfms.fcp.FcpStatusListener;

public class FmsManager {
	private static final FmsManager instance = new FmsManager();

	private Store store;
	private final BoardManager boardManager;;
	private final IdentityManager identityManager;
	private final MessageManager messageManager;
	private final TrustManager trustManager;
	private FcpClient fcpClient;
	private MessageDownloader messageDownloader;
	private InsertThread insertThread;
	private boolean isOffline;

	public static FmsManager getInstance() {
		return instance;
	}

	private FmsManager() {
			boardManager = new BoardManager();
			identityManager = new IdentityManager();
			messageManager = new MessageManager();
			trustManager = new TrustManager();
	}

	public void initialize(List<String> seedIdentities) throws SQLException{
		final Config config = Config.getInstance();

		isOffline = config.getOffline();
		fcpClient = new FcpClient("jfms-fms",
				config.getFcpHost(), config.getFcpPort());
		messageDownloader = new MessageDownloader(fcpClient);
		insertThread = new InsertThread(fcpClient);
		store = new Store();
		store.initialize(seedIdentities);
		boardManager.initialize();
		identityManager.initialize();
		trustManager.initialize();
	}

	public void initialize() throws SQLException {
		initialize(null);
	}

	public void shutdown() {
		insertThread.cancel();
		messageDownloader.cancel();
	}

	public void setOffline(boolean isOffline) {
		this.isOffline = isOffline;
		Config.getInstance().setStringValue(Config.OFFLINE,
				Boolean.toString(isOffline));
		Config.getInstance().saveToFile(Constants.JFMS_CONFIG_PATH);
	}

	public boolean isOffline() {
		return isOffline;
	}

	public void startBackgroundThread() {
		// TODO online/offline handling
		new Thread(insertThread).start();
		new Thread(messageDownloader).start();
	}

	public Store getStore() {
		return store;
	}

	public BoardManager getBoardManager() {
		return boardManager;
	}

	public IdentityManager getIdentityManager() {
		return identityManager;
	}

	public MessageManager getMessageManager() {
		return messageManager;
	}

	public TrustManager getTrustManager() {
		return trustManager;
	}

	public ReadOnlyStringProperty getProgressTitleProperty() {
		return messageDownloader.titleProperty();
	}

	public ReadOnlyDoubleProperty getProgressProperty() {
		return messageDownloader.progressProperty();
	}

	public ReadOnlyStringProperty getStatusTextProperty() {
		return messageDownloader.messageProperty();
	}

	public void setFcpStatusListener(FcpStatusListener listener) {
		fcpClient.setStatusListener(listener);
	}
}
