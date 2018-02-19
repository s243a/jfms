package jfms.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class MessageBodyTextView implements MessageBodyView {
	private final TextArea textArea = new TextArea();
	private String body;
	private String signature;
	private boolean muteQuotes = false;
	private boolean showSignature = false;
	private final Font normalFont;
	private final Font monospacFont;


	public MessageBodyTextView() {
		textArea.setPrefColumnCount(80);
		textArea.setPrefRowCount(25);
		textArea.setWrapText(true);
		textArea.setEditable(false);

		normalFont = textArea.getFont();
		monospacFont = Font.font("Monospaced", FontWeight.NORMAL, 13);
	}

	@Override
	public Node getNode() {
		return textArea;
	}

	@Override
	public void setText(String body, String signature) {
		this.body = body;
		this.signature = signature;

		renderMessage();
	}

	@Override
	public void setMuteQuotes(boolean muteQuotes) {
		this.muteQuotes = muteQuotes;
		renderMessage();
	}

	@Override
	public void setShowEmoticons(boolean showEmoticons) {
	}

	@Override
	public void setShowSignature(boolean showSignature) {
		this.showSignature = showSignature;
		renderMessage();
	}

	@Override
	public void setUseMonospaceFont(boolean useMonospaceFont) {
		if (useMonospaceFont) {
			textArea.setFont(monospacFont);
		} else {
			textArea.setFont(normalFont);
		}

		renderMessage();
	}

	private void renderMessage() {
		if (!muteQuotes) {
			textArea.setText(body);
		} else {
			try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
				StringBuilder str = new StringBuilder();
				boolean firstLine = true;
				boolean inQuote = false;

				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith(">")) {
						if (!inQuote) {
							if (!firstLine) {
								str.append('\n');
							}
							str.append("> [quoted text muted]");
						}

						inQuote = true;
					} else {
						inQuote = false;

						if (!firstLine) {
							str.append('\n');
						}
						str.append(line);
					}

					firstLine = false;
				}

				textArea.setText(str.toString());
			} catch (IOException e) {
			}
		}

		if (showSignature && signature != null) {
			textArea.appendText("\n\n-- \n" + signature);
		}
	}
}
