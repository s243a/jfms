package jfms.ui;

import javafx.scene.Node;

interface MessageBodyView {
	Node getNode();
	void setText(String value, String signature);
	void setMuteQuotes(boolean muteQuotes);
	void setUseMonospaceFont(boolean useMonospaceFont);
	void setShowEmoticons(boolean showEmoticons);
	void setShowSignature(boolean muteQuotes);
}
