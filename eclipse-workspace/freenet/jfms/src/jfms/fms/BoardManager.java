package jfms.fms;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BoardManager {
	private static final Logger LOG = Logger.getLogger(BoardManager.class.getName());

	private Map<String, Integer> nameToBoardId;

	public void initialize() {
		nameToBoardId = FmsManager.getInstance().getStore().getBoardNames();
	}

	public void addBoard(Integer boardId, String boardName) {
		nameToBoardId.put(boardName, boardId);
	}

	public Integer getBoardId(String name) {
		return nameToBoardId.get(name);
	}

	public Collection<String> getBoardNames() {
		return nameToBoardId.keySet();
	}

	public String getBoardName(int boardId) {
		// TODO reverse map
		for (Map.Entry<String, Integer> e : nameToBoardId.entrySet()) {
			if (e.getValue() == boardId) {
				return e.getKey();
			}
		}

		return null;
	}

	public List<String> getSubscribedBoardNames() {
		final Store store = FmsManager.getInstance().getStore();
		return store.getSubscribedBoardNames();
	}

	public int getUnreadMessageCount(String boardName) {
		final Store store = FmsManager.getInstance().getStore();
		final int boardId = getBoardId(boardName);

		return store.getUnreadMessageCount(boardId);
	}

	public void subscribe(String name) {
		Store store = FmsManager.getInstance().getStore();

		Integer boardId = nameToBoardId.get(name);
		if (boardId == null) {
			LOG.log(Level.WARNING, "board {0} not found", name);
			return;
		}

		store.setBoardSubscribed(boardId, true);
	}

	public void unsubscribe(String name) {
		Store store = FmsManager.getInstance().getStore();

		Integer boardId = nameToBoardId.get(name);
		if (boardId == null) {
			LOG.log(Level.WARNING, "board {0} not found", name);
			return;
		}

		store.setBoardSubscribed(boardId, false);
	}

	public void setBoardMessagesRead(String boardName, boolean read) {
		final Store store = FmsManager.getInstance().getStore();
		final int boardId = getBoardId(boardName);

		store.setBoardMessagesRead(boardId, read);
	}
}
