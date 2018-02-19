package jfms.fms;

import java.util.Map;
import java.util.TreeMap;

public class InReplyTo {
	private final Map<Integer, String> messages = new TreeMap<>();

	public void add(int order, String messageId) {
		if (messages.containsKey(order)) {
			// TODO use less general exception
			throw new RuntimeException("duplicate InReplyTo order");
		}

		messages.put(order, messageId);
	}

	public Map<Integer, String> getMessages() {
		return messages;
	}

	public String getParentMessageId() {
		return messages.get(0);
	}

	public InReplyTo increment() {
		InReplyTo inReplyTo = new InReplyTo();
		messages.entrySet().stream()
			.forEach(e -> inReplyTo.add(e.getKey() + 1, e.getValue()));

		return inReplyTo;
	}
};
