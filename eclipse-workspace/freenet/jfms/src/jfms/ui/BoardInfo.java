package jfms.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class BoardInfo {
	private final IntegerProperty storeId = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final BooleanProperty isSubscribed = new SimpleBooleanProperty();
	private final IntegerProperty messageCount = new SimpleIntegerProperty();

	public final int getStoreId() {
		return storeId.get();
	}

	public final void setStoreId(int storeId) {
		this.storeId.set(storeId);
	}

	public final IntegerProperty storeIdProperty() {
		return storeId;
	}

	public final String getName() {
		return name.get();
	}

	public final void setName(String name) {
		this.name.set(name);
	}

	public final StringProperty nameProperty() {
		return name;
	}

	public final boolean getIsSubscribed() {
		return isSubscribed.get();
	}

	public final void setIsSubscribed(boolean isSubscribed) {
		this.isSubscribed.set(isSubscribed);
	}

	public final BooleanProperty isSubscribedProperty() {
		return isSubscribed;
	}

	public final int getMessageCount() {
		return messageCount.get();
	}

	public final void setMessageCount(int messageCount) {
		this.messageCount.set(messageCount);
	}

	public final IntegerProperty messageCountProperty() {
		return messageCount;
	}

}
