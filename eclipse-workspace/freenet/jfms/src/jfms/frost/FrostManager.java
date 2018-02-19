package jfms.frost;

public class FrostManager {
	private static final FrostManager instance = new FrostManager();

	private final Store store;
	private final BoardManager boardManager;
	private final MessageDownloader messageDownloader;

	public static FrostManager getInstance() {
		return instance;
	}

	private FrostManager() {
		store = new Store();
		boardManager = new BoardManager();
		messageDownloader = new MessageDownloader();
	}

	public void initialize() {
		store.initialize();
	}

	public void shutdown() {
		messageDownloader.cancel();
	}

	public void startBackgroundThread() {
		new Thread(messageDownloader).start();
	}

	public Store getStore() {
		return store;
	}

	public BoardManager getBoardManager() {
		return boardManager;
	}
}
