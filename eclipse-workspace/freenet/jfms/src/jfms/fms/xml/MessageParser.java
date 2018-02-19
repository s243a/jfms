package jfms.fms.xml;

import java.io.InputStream;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jfms.fms.Attachment;
import jfms.fms.InReplyTo;
import jfms.fms.Message;
import jfms.fms.Sanitizer;
import jfms.fms.Validator;

public class MessageParser {
	private static final Logger LOG = Logger.getLogger(MessageParser.class.getName());

	public Message parse(InputStream is) {
		try {
			XMLStreamReader xmlReader = Utils.createXMLStreamReader(is);

			return parse(xmlReader);
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to parse message", e);
			return null;
		}
	}

	public Message parse(Reader reader) {
		try {
			XMLStreamReader xmlReader = Utils.createXMLStreamReader(reader);

			return parse(xmlReader);
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to parse message", e);
			return null;
		}
	}

	private Message parse(XMLStreamReader reader) throws XMLStreamException {
		Message message = null;

		while (reader.hasNext()) {
			if (reader.next() == XMLStreamConstants.START_ELEMENT) {
				if (reader.getLocalName().equals("Message")) {
					message = parseMessage(reader);
				}
				break;
			}
		}

		return message;
	}

	private Message parseMessage(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		Message msg = new Message();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					switch (reader.getLocalName()) {
					case "Date":
						try {
							msg.setDate(LocalDate.parse(reader.getElementText(),
									DateTimeFormatter.ISO_LOCAL_DATE));
						} catch (DateTimeParseException e) {
							LOG.log(Level.WARNING, "Failed to parse date");
						}
						level--;
						break;
					case "Time":
						try {
							msg.setTime(LocalTime.parse(reader.getElementText(),
									DateTimeFormatter.ISO_LOCAL_TIME));
						} catch (DateTimeParseException e) {
							LOG.log(Level.WARNING, "Failed to parse time");
						}
						level--;
						break;
					case "Subject":
						msg.setSubject(reader.getElementText());
						level--;
						break;
					case "MessageID":
						final String uuid = reader.getElementText();
						if (Validator.isValidUuid(uuid)) {
							msg.setMessageUuid(uuid);
						} else {
							LOG.log(Level.WARNING, "invalid UUID in Message");
						}
						level--;
						break;
					case "ReplyBoard":
						final String sanitized = Sanitizer.sanitizeBoard(
								reader.getElementText());
						if (!sanitized.isEmpty()) {
							msg.setReplyBoard(sanitized);
						}
						level--;
						break;
					case "Body":
						msg.setBody(reader.getElementText());
						level--;
						break;
					case "InReplyTo":
						InReplyTo inReplyTo = parseInReplyTo(reader);
						if (!inReplyTo.getMessages().isEmpty()) {
							msg.setInReplyTo(inReplyTo);
						}
						level--;
						break;
					case "Boards":
						List<String> boards = parseBoards(reader);
						level--;
						msg.setBoards(boards);
						break;
					case "Attachments":
						List<Attachment> attachments = parseAttachments(reader);
						if (!attachments.isEmpty()) {
							msg.setAttachments(attachments);
						}
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

		boolean valid =
			msg.getDate() != null &&
			msg.getTime() != null &&
			msg.getSubject() != null && !msg.getSubject().isEmpty() &&
			msg.getMessageUuid() != null &&
			msg.getReplyBoard() != null && !msg.getReplyBoard().isEmpty() &&
			msg.getBoards() != null && !msg.getBoards().isEmpty();

		if (!valid) {
			LOG.log(Level.WARNING, "skipping invalid message");
			return null;
		}

		return msg;
	}

	private List<String> parseBoards(XMLStreamReader reader) throws XMLStreamException {
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

	private InReplyTo parseInReplyTo(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		InReplyTo inReplyTo = new InReplyTo();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2 && reader.getLocalName().equals("Message")) {
					parseInReplyToMessage(reader, inReplyTo);
					level--;
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		return inReplyTo;
	}

	private void parseInReplyToMessage(XMLStreamReader reader, InReplyTo inReplyTo) throws XMLStreamException {
		int level = 1;
		int order = -1;
		String messageId = null;

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					switch (reader.getLocalName()) {
					case "Order":
						try {
							order = Integer.parseInt(reader.getElementText());
						} catch (NumberFormatException e) {
							LOG.log(Level.WARNING, "Failed to parse order");
						}
						level--;
						break;
					case "MessageID":
						final String uuid = reader.getElementText();
						if (Validator.isValidUuid(uuid)) {
							messageId = uuid;
						} else {
							LOG.log(Level.WARNING, "invalid UUID in ReplyTo");
						}
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

		if (order >= 0 && messageId != null) {
			inReplyTo.add(order, messageId);
		}
	}

	private List<Attachment> parseAttachments(XMLStreamReader reader)
		throws XMLStreamException {

		List<Attachment> attachments = new ArrayList<>();
		int level = 1;

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2 && reader.getLocalName().equals("File")) {
					Attachment attachment = parseFile(reader);
					if (attachment != null) {
						attachments.add(attachment);
					}
					level--;
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		return attachments;
	}

	private Attachment parseFile(XMLStreamReader reader)
		throws XMLStreamException {

		String key = null;
		int size = -1;
		int level = 1;

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					switch (reader.getLocalName()) {
					case "Key":
						final String uri = reader.getElementText();
						if (Validator.isValidFreenetURI(uri)) {
							key = uri;
						} else {
							LOG.log(Level.WARNING, "invalid URI in key");
						}
						level--;
						break;
					case "Size":
						try {
							size = Integer.parseInt(reader.getElementText());
						} catch (NumberFormatException e) {
							LOG.log(Level.WARNING, "Failed to parse size");
						}
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

		if (key != null && size >=  0) {
			return new Attachment(key, size);
		} else {
			return null;
		}
	}
}
