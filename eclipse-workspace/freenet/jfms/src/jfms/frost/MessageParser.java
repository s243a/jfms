package jfms.frost;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


public class MessageParser {
	private static final Logger LOG = Logger.getLogger(MessageParser.class.getName());

	public Message parse(InputStream is) {
		Message message = null;

		try {
			XMLStreamReader reader = jfms.fms.xml.Utils.createXMLStreamReader(is);

			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT) {
					if (reader.getLocalName().equals("FrostMessage")) {
						message = parseFrostMessage(reader);
					}
					break;
				}
			}
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to parse message", e);
		}

		return message;
	}

	private Message parseFrostMessage(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		Message message = new Message();
		String date = "";
		String time = "";

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					switch (reader.getLocalName()) {
					case "MessageId":
						message.setMessageId(reader.getElementText());
						level--;
						break;
					case "InReplyTo":
						final String inReplyTo = reader.getElementText();
						final int len = inReplyTo.length();
						level--;
						if (len >= 64) {
							message.setParentMessageId(inReplyTo.substring(len-64));
						}
						break;
					case "From":
						message.setFrom(reader.getElementText());
						level--;
						break;
					case "Subject":
						message.setSubject(reader.getElementText());
						level--;
						break;
					case "Date":
						date = reader.getElementText();
						level--;
						break;
					case "Time":
						time = reader.getElementText();
						level--;
						break;
					case "Body":
						message.setBody(reader.getElementText());
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

		message.setDate(date + ' ' + time);

		return message;
	}
}
