package jfms.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Board {
	// XXX properties are currently unsed
	private final StringProperty name = new SimpleStringProperty();
	private final BooleanProperty isFolder = new SimpleBooleanProperty();
	private final IntegerProperty unreadMessageCount = new SimpleIntegerProperty();

	public Board(String folderName)
	{
		this.name.set(folderName);
		this.isFolder.set(true);
	}

	public Board(String boardName, int unreadMessageCount)
	{
		this.name.set(boardName);
		this.isFolder.set(false);
		this.unreadMessageCount.set(unreadMessageCount);
	}

	public final String getName() {
		return name.get();
	}

	public StringProperty nameProperty() {
		return name;
	}

	public final boolean isFolder() {
		return isFolder.get();
	}

	public BooleanProperty isFolderProperty() {
		return isFolder;
	}

	public int getUnreadMessageCount() {
		return unreadMessageCount.get();
	}

	public IntegerProperty unreadMessageCountProperty() {
		return unreadMessageCount;
	}
}
