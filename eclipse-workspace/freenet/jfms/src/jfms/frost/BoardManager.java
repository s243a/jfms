package jfms.frost;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BoardManager {
	private static final Logger LOG = Logger.getLogger(BoardManager.class.getName());

	private final List<Board> boardList = new ArrayList<>();

	BoardManager() {
	}

	public void add(Board board) {
		LOG.log(Level.FINEST, "adding frost board {0}", board.getName());
		boardList.add(board);
	}

	public Board getBoard(String name) {
		Board board = null;
		for (Board b : boardList) {
			if (name.equals(b.getName())) {
				board = b;
				break;
			}
		}

		return board;
	}

	public List<Board> getBoardList() {
		return boardList;
	}
}
