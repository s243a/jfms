package jfms.ui;

import javafx.scene.Node;

public interface WizardPage {
	boolean commit();
	boolean hasNext();
	String getHeader();
	Node getContent();
	WizardPage nextPage();
}
