package jfms.fms.xml;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jfms.fms.MessageReference;
import jfms.fms.Sanitizer;
import jfms.fms.Validator;

public class MessageListParser {
	private static final Logger LOG = Logger.getLogger(MessageListParser.class.getName());

	public List<MessageReference> parse(InputStream is) {
		List<MessageReference> messageList = null;

		try {
			XMLStreamReader reader = Utils.createXMLStreamReader(is);

			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT) {
					if (reader.getLocalName().equals("MessageList")) {
						messageList = parseMessageList(reader);
					}
					break;
				}
			}
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to parse message list", e);
		}

		if (messageList == null) {
			messageList = Collections.emptyList();
		}
		return messageList;
	}

	private List<MessageReference> parseMessageList(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		List<MessageReference> refs = new ArrayList<>();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					final String localName = reader.getLocalName();
					if (localName.equals("Message")) {
						MessageReference message = parseMessage(reader, false);
						level--;

						if (message != null) {
							refs.add(message);
						}
					} else if (localName.equals("ExternalMessage")) {
						MessageReference message = parseMessage(reader, true);
						level--;

						if (message != null) {
							refs.add(message);
						}
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		return refs;
	}

	private MessageReference parseMessage(XMLStreamReader reader,
			boolean isExternal) throws XMLStreamException {

		int level = 1;
		MessageReference messageRef = new MessageReference();
		boolean isKeyed = false;

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					switch (reader.getLocalName()) {
					case "Type":
						isKeyed = reader.getElementText().equals("Keyed");
						level--;
						break;
					case "Identity":
						final String ssk = reader.getElementText();
						if (isExternal) {
							if (Validator.isValidSsk(ssk)) {
								messageRef.setSsk(ssk);
							} else {
								LOG.log(Level.WARNING, "invalid SSK in message list");
							}
						}
						level--;
						break;
					case "Date":
						try {
							messageRef.setDate(LocalDate.parse(
									reader.getElementText(),
									DateTimeFormatter.ISO_LOCAL_DATE));
						} catch (DateTimeParseException e) {
							LOG.log(Level.WARNING, "Failed to parse date");
						}
						level--;
						break;
					case "Index":
						try {
							messageRef.setIndex(
									Integer.parseInt(reader.getElementText()));
						} catch (NumberFormatException e) {
							LOG.log(Level.WARNING, "Failed to parse index");
						}
						level--;
						break;
					case "Boards":
						messageRef.setBoards(parseBoards(reader));
						level--;
						break;
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		// allow empty boards here; authoritative board list is in message XML
		boolean valid =
			messageRef.getDate() != null &&
			messageRef.getIndex() >= 0;

		if (valid && isExternal) {
			valid = isKeyed && messageRef.getSsk() != null;
		}

		if (!valid) {
			LOG.log(Level.WARNING, "skipping invalid message in messagelist");
			return null;
		}

		return messageRef;
	}

	private List<String> parseBoards(XMLStreamReader reader)
		throws XMLStreamException {

		int level = 1;
		List<String> boards = new ArrayList<>();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2 && reader.getLocalName().equals("Board")) {
					final String sanitized = Sanitizer.sanitizeBoard(
							reader.getElementText());
					if (!sanitized.isEmpty()) {
						boards.add(sanitized);
					}
					level--;
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		return boards;
	}
}
