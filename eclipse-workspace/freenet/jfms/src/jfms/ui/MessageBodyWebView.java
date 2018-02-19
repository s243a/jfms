package jfms.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

class MessageBodyWebView implements MessageBodyView {
	private final WebView webView = new WebView();
	private final WebEngine webEngine;
	private String body;
	private String signature;
	private boolean muteQuotes = false;
	private boolean showSignature = false;
	private boolean useMonospaceFont = false;
	private boolean showEmoticons = false;

	private static void encodeHTML(StringBuilder out, String str)
	{
		for (int i = 0; i < str.length(); i++) {
			final char c = str.charAt(i);
			if (c > 0x7f || c=='"' || c=='&' || c=='<' || c=='>') {
				out.append("&#");
				out.append((int)c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
	}

	public MessageBodyWebView() {
		webEngine = webView.getEngine();
	}

	@Override
	public Node getNode() {
		return webView;
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
		this.showEmoticons = showEmoticons;
		renderMessage();
	}

	@Override
	public void setShowSignature(boolean showSignature) {
		this.showSignature = showSignature;
		renderMessage();
	}

	@Override
	public void setUseMonospaceFont(boolean useMonospaceFont) {
		this.useMonospaceFont = useMonospaceFont;
		renderMessage();
	}

	private void renderMessage() {
		if (body == null) {
			webEngine.loadContent("<html/>");
			return;
		}

		StringBuilder str = new StringBuilder();
		str.append("<html>\n");
		str.append("<head>\n");
		str.append("<style type=\"text/css\">\n");
		str.append(".quote { color: green; }\n");
		str.append(".signature { color: gray; }\n");
		str.append("</style>\n");
		str.append("</head>\n");
		str.append("<body>\n");

		if (useMonospaceFont) {
			str.append("<pre>");
		}

		try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
			boolean firstLine = true;
			boolean inQuote = false;
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(">")) {
					if (!inQuote) {
						str.append("<span class=\"quote\">");
						if (muteQuotes) {
							if (!firstLine) {
								addNewline(str);
							}
							str.append("&gt; [quoted text muted]");
						}
					}
					inQuote = true;
				} else {
					if (inQuote) {
						str.append("</span>");
					}
					inQuote = false;
				}

				if (!muteQuotes || !inQuote) {
					if (!firstLine) {
						addNewline(str);
					}
					encodeHTML(str, line);
				}

				firstLine = false;
			}

			if (inQuote) {
				str.append("</span>");
			}
		} catch (IOException e) {
		}


		if (showSignature && signature != null) {
			addNewline(str);
			addNewline(str);
			str.append("<span class=\"signature\">");
			str.append("--&nbsp;");

			try (BufferedReader reader = new BufferedReader(new StringReader(signature))) {
				String line;
				while ((line = reader.readLine()) != null) {
					addNewline(str);
					encodeHTML(str, line);
				}
			} catch (IOException e) {
			}

			str.append("</span>");
		}

		if (useMonospaceFont) {
			str.append("</pre>");
		}
		str.append("\n</body>\n");
		str.append("</html>");

		String html = str.toString();

		if (showEmoticons) {
			html = Emoticons.getInstance().replaceByImgTags(str.toString());
		}

		webEngine.loadContent(html);
	}

	private void addNewline(StringBuilder out) {
		out.append('\n');
		if (!useMonospaceFont) {
			out.append("<br>");
		}
	}
}
