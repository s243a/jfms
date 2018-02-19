package jfms.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Identity {
	private final IntegerProperty id = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty ssk = new SimpleStringProperty();
	private final IntegerProperty trustListTrust = new SimpleIntegerProperty();
	private final IntegerProperty messageTrust = new SimpleIntegerProperty();

	public final int getId() {
		return id.get();
	}

	public final void setId(int id) {
		this.id.set(id);
	}

	public final IntegerProperty idProperty() {
		return id;
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

	public final String getSsk() {
		return ssk.get();
	}

	public final void setSsk(String ssk) {
		this.ssk.set(ssk);
	}

	public final StringProperty sskProperty() {
		return ssk;
	}

	public final int getTrustListTrust() {
		return trustListTrust.get();
	}

	public final void setTrustListTrust(int trustListTrust) {
		this.trustListTrust.set(trustListTrust);
	}

	public final IntegerProperty trustListTrustProperty() {
		return trustListTrust;
	}

	public final int getMessageTrust() {
		return messageTrust.get();
	}

	public final void setMessageTrust(int messageTrust) {
		this.messageTrust.set(messageTrust);
	}

	public final IntegerProperty messageTrustProperty() {
		return messageTrust;
	}
}
