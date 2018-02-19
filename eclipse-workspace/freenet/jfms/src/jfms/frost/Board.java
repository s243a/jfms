package jfms.frost;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;


public class Board {
	private final String name;
	private final ObservableList<Message> messageList = FXCollections.observableArrayList();

	public Board(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addMessage(Message message) {
		messageList.add(message);
	}

	public List<Message> getMessages() {
		return messageList;
	}

	public void addListener(ListChangeListener<Message> listener) {
		messageList.addListener(listener);
	}

	public void removeListener(ListChangeListener<Message> listener) {
		messageList.removeListener(listener);
	}
}
