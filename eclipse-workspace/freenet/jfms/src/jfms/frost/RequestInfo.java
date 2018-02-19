package jfms.frost;

public class RequestInfo {
	private final String boardName;
	private final String date;
	private int index;

	public RequestInfo(String boardName, String date, int index) {
		this.boardName = boardName;
		this.date = date;
		this.index = index;
	}

	public String getBoardName() {
		return boardName;
	}

	public String getDate() {
		return date;
	}

	public int getIndex() {
		return index;
	}

	public void incrementIndex() {
		index++;
	}

	public String getMessageKey() {
		StringBuilder key = new StringBuilder("KSK@frost|message|news|");
		key.append(date);
		key.append('-');
		key.append(boardName);
		key.append('-');
		key.append(index);
		key.append(".xml");

		return key.toString();
	}
}
