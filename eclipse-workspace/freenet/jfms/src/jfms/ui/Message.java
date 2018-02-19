package jfms.ui;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Message {
	private static final Logger LOG = Logger.getLogger(Message.class.getName());

	public enum Status {
		UNREAD,
		READ
	}

	private int storeId;
	private int identityId;

	private final StringProperty subject = new SimpleStringProperty();
	private final StringProperty from = new SimpleStringProperty();
	private final StringProperty fromShort = new SimpleStringProperty();
	private final StringProperty date = new SimpleStringProperty();
	private final StringProperty messageId = new SimpleStringProperty();
	private final StringProperty parentMessageId = new SimpleStringProperty();
	private final IntegerProperty trustLevel = new SimpleIntegerProperty();
	private final StringProperty indexDate = new SimpleStringProperty();
	private final IntegerProperty index = new SimpleIntegerProperty();
	private final StringProperty replyBoard = new SimpleStringProperty();
	private final StringProperty boards = new SimpleStringProperty();
	private final StringProperty body = new SimpleStringProperty();
	private final BooleanProperty read = new SimpleBooleanProperty();

	private List<String> boardList;

	public int getStoreId() {
		return storeId;
	}

	public void setStoreId(int dbId) {
		this.storeId = dbId;
	}

	public int getIdentityId() {
		return identityId;
	}

	public void setIdentityId(int identityId) {
		this.identityId = identityId;
	}

	public final String getSubject() {
		return subject.get();
	}

	public final void setSubject(String subject) {
		this.subject.set(subject);
	}

	public final StringProperty subjectProperty() {
		return subject;
	}

	public final String getFrom() {
		return from.get();
	}

	public final void setFrom(String from) {
		this.from.set(from);
		// TODO is @ allowed in identity name?
		if (from != null) {
			this.fromShort.set(from.replaceFirst("@.*", ""));
		} else {
			this.fromShort.set(null);
		}
	}

	public final StringProperty fromProperty() {
		return from;
	}

	public final StringProperty fromShortProperty() {
		return fromShort;
	}

	public final String getDate() {
		return date.get();
	}

	public final void setDate(String date) {
		this.date.set(date);
	}

	public final StringProperty dateProperty() {
		return date;
	}

	public final String getMessageId() {
		return messageId.get();
	}

	public final void setMessageId(String messageId) {
		this.messageId.set(messageId);
	}

	public final StringProperty messageIdProperty() {
		return messageId;
	}

	public final String getParentMessageId() {
		return parentMessageId.get();
	}

	public final void setParentMessageId(String parentMessageId) {
		this.parentMessageId.set(parentMessageId);
	}

	public final StringProperty parentMessageIdProperty() {
		return parentMessageId;
	}

	public final int getTrustLevel() {
		return trustLevel.get();
	}

	public final void setTrustLevel(int trustLevel) {
		this.trustLevel.set(trustLevel);
	}

	public final IntegerProperty trustLevelProperty() {
		return trustLevel;
	}

	public final String getIndexDate() {
		return indexDate.get();
	}

	public final void setIndexDate(String indexDate) {
		this.indexDate.set(indexDate);
	}

	public final StringProperty indexDateProperty() {
		return indexDate;
	}

	public final int getIndex() {
		return index.get();
	}

	public final void setIndex(int index) {
		this.index.set(index);
	}

	public final IntegerProperty indexProperty() {
		return index;
	}

	public final String getReplyBoard() {
		return replyBoard.get();
	}

	public final void setReplyBoard(String replyBoard) {
		this.replyBoard.set(replyBoard);
	}

	public final StringProperty replyBoardProperty() {
		return replyBoard;
	}

	public List<String> getBoardList() {
		return boardList;
	}

	public final void setBoardList(List<String> boardList) {
		this.boardList = boardList;
	}

	public final String getBoards() {
		return boards.get();
	}

	public final void setBoards() {
		if (boardList == null) {
			LOG.log(Level.WARNING, "boardList not set");
			return;
		}

		// XXX copied from MessageWindow
		// TODO sort instead of random DB order
		StringBuilder str = new StringBuilder();
		str.append(getReplyBoard());
		for (String board : boardList) {
			if (!board.equals(getReplyBoard())) {
				str.append(", ");
				str.append(board);
			}
		}

		boards.set(str.toString());
	}

	public final StringProperty boardsProperty() {
		return boards;
	}

	public final String getBody() {
		return body.get();
	}

	public final void setBody(String body) {
		this.body.set(body);
	}

	public final StringProperty bodyProperty() {
		return body;
	}

	public final boolean getRead() {
		return read.get();
	}

	public final void setRead(boolean read) {
		this.read.set(read);
	}

	public final BooleanProperty readProperty() {
		return read;
	}
}
