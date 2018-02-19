package jfms.fms.xml;

import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jfms.fms.Message;
import jfms.fms.Sanitizer;
import jfms.fms.Validator;

public class MessageWriter {
	private static final Logger LOG = Logger.getLogger(MessageWriter.class.getName());

	public String writeXml(Message msg) {
		StringWriter strWriter = new StringWriter();

		if (!Validator.isValidUuid(msg.getMessageUuid())) {
			LOG.log(Level.WARNING, "invalid message ID");
			return null;
		}

		final String sanitizedReplyBoard =
				Sanitizer.sanitizeBoard(msg.getReplyBoard());
		if (sanitizedReplyBoard.isEmpty()) {
			LOG.log(Level.WARNING, "reply board empty");
			return null;
		}

		List<String> sanitizedBoards = new ArrayList<>(msg.getBoards().size());
		for (String board : msg.getBoards()) {
			final String sanitized = Sanitizer.sanitizeBoard(board);
			if (!sanitized.isEmpty()) {
				sanitizedBoards.add(sanitized);
			}
		}
		if (sanitizedBoards.isEmpty()) {
			LOG.log(Level.WARNING, "board list empty");
			return null;
		}

		try {
			XMLStreamWriter xtw = Utils.createXMLStreamWriter(strWriter);

			xtw.writeStartDocument("UTF-8", "1.0");

			xtw.writeStartElement("Message");

			xtw.writeStartElement("Date");
			xtw.writeCharacters(msg.getDate()
					.format(DateTimeFormatter.ISO_LOCAL_DATE));
			xtw.writeEndElement();

			xtw.writeStartElement("Time");
			xtw.writeCharacters(msg.getTime()
					.format(DateTimeFormatter.ISO_LOCAL_TIME));
			xtw.writeEndElement();

			xtw.writeStartElement("Subject");
			xtw.writeCharacters(msg.getSubject());
			xtw.writeEndElement();

			xtw.writeStartElement("MessageID");
			xtw.writeCharacters(msg.getMessageUuid());
			xtw.writeEndElement();

			xtw.writeStartElement("ReplyBoard");
			xtw.writeCharacters(sanitizedReplyBoard);
			xtw.writeEndElement();

			xtw.writeStartElement("Body");
			xtw.writeCData(msg.getBody());
			xtw.writeEndElement();

			xtw.writeStartElement("Boards");
			for (String board : sanitizedBoards) {
				xtw.writeStartElement("Board");
				xtw.writeCharacters(board);
				xtw.writeEndElement();
			}
			xtw.writeEndElement();

			Map<Integer, String> messages = null;
			if (msg.getInReplyTo() != null) {
				messages = msg.getInReplyTo().getMessages();
			}

			if (messages != null && !messages.isEmpty()) {
				xtw.writeStartElement("InReplyTo");
				for (Map.Entry<Integer, String> e : messages.entrySet()) {
					if (!Validator.isValidUuid(e.getValue())) {
						LOG.log(Level.WARNING, "invalid UUID in ReplyTo");
						continue;
					}
					xtw.writeStartElement("Message");

					xtw.writeStartElement("Order");
					xtw.writeCharacters(e.getKey().toString());
					xtw.writeEndElement();

					xtw.writeStartElement("MessageID");
					xtw.writeCharacters((e.getValue()));
					xtw.writeEndElement();

					xtw.writeEndElement();
				}
				xtw.writeEndElement();
			}

			xtw.writeEndDocument();

			xtw.flush();
			xtw.close();
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to create MesssageList", e);
			return null;
		}

		return strWriter.toString();
	}
}
