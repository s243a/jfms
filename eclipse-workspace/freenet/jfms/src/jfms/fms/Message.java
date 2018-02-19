package jfms.fms;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class Message {
	// TODO remove message id from here and pass to newMessage()
	private int messageId;
	private int identityId;
	private LocalDate date;
	private LocalTime time;
	private String subject;
	private String messageUuid;
	private String replyBoard;
	private LocalDate insertDate;
	private int insertIndex = -1;
	private String body;

	private List<String> boards;
	private List<Attachment> attachments;
	private InReplyTo inReplyTo;
	private String parentId;
	private boolean read;

	// TODO remove?
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof Message)) {
			return false;
		}

		Message m = (Message)o;
		return Objects.equals(messageUuid, m.messageUuid);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(messageUuid);
	}

	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public int getIdentityId() {
		return identityId;
	}

	public void setIdentityId(int identityId) {
		this.identityId = identityId;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessageUuid() {
		return messageUuid;
	}

	public void setMessageUuid(String messageUuid) {
		this.messageUuid = messageUuid;
	}

	public void setReplyBoard(String replyBoard) {
		this.replyBoard = replyBoard;
	}

	public String getReplyBoard() {
		return replyBoard;
	}

	public LocalDate getInsertDate() {
		return insertDate;
	}

	public void setInsertDate(LocalDate insertDate) {
		this.insertDate = insertDate;
	}

	public int getInsertIndex() {
		return insertIndex;
	}

	public void setInsertIndex(int insertIndex) {
		this.insertIndex = insertIndex;
	}

	public void setBody (String body) {
		this.body = body;
	}

	public String getBody() {
		return body;
	}

	public List<String> getBoards() {
		return boards;
	}

	public void setBoards(List<String> boards) {
		this.boards = boards;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public void setInReplyTo(InReplyTo inReplyTo) {
		this.inReplyTo = inReplyTo;
	}

	public InReplyTo getInReplyTo() {
		return inReplyTo;
	}

	public boolean getRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getParentId() {
		return parentId;
	}
}
