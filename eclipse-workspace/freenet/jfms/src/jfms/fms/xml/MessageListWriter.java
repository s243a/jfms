package jfms.fms.xml;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jfms.fms.MessageReference;
import jfms.fms.Sanitizer;

public class MessageListWriter {
	private static final Logger LOG = Logger.getLogger(MessageListWriter.class.getName());

	public byte[] writeXml(List<MessageReference> messageList,
			List<MessageReference> externalMessageList) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			XMLStreamWriter xtw = Utils.createXMLStreamWriter(bos, "UTF-8");

			xtw.writeStartDocument("UTF-8", "1.0");
			xtw.writeStartElement("MessageList");

			for (MessageReference msgRef : messageList) {
				xtw.writeStartElement("Message");

				xtw.writeStartElement("Date");
				xtw.writeCharacters(msgRef.getDate()
						.format(DateTimeFormatter.ISO_LOCAL_DATE));
				xtw.writeEndElement();

				xtw.writeStartElement("Index");
				xtw.writeCharacters(Integer.toString(msgRef.getIndex()));
				xtw.writeEndElement();

				xtw.writeStartElement("Boards");
				for (String board : msgRef.getBoards()) {
					final String sanitized = Sanitizer.sanitizeBoard(board);
					if (!sanitized.isEmpty()) {
						xtw.writeStartElement("Board");
						xtw.writeCharacters(sanitized);
						xtw.writeEndElement();
					}
				}
				xtw.writeEndElement();

				xtw.writeEndElement();
			}

			for (MessageReference msgRef : externalMessageList) {
				xtw.writeStartElement("ExternalMessage");

				xtw.writeStartElement("Type");
				xtw.writeCharacters("Keyed");
				xtw.writeEndElement();

				xtw.writeStartElement("Identity");
				xtw.writeCharacters(msgRef.getSsk());
				xtw.writeEndElement();

				xtw.writeStartElement("Date");
				xtw.writeCharacters(msgRef.getDate()
						.format(DateTimeFormatter.ISO_LOCAL_DATE));
				xtw.writeEndElement();

				xtw.writeStartElement("Index");
				xtw.writeCharacters(Integer.toString(msgRef.getIndex()));
				xtw.writeEndElement();

				xtw.writeStartElement("Boards");
				for (String board : msgRef.getBoards()) {
					final String sanitized = Sanitizer.sanitizeBoard(board);
					if (!sanitized.isEmpty()) {
						xtw.writeStartElement("Board");
						xtw.writeCharacters(sanitized);
						xtw.writeEndElement();
					}
				}
				xtw.writeEndElement();

				xtw.writeEndElement();
			}

			xtw.writeEndElement();
			xtw.writeEndDocument();

			xtw.flush();
			xtw.close();

		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to create MesssageList", e);
			return null;
		}

		return bos.toByteArray();
	}
}
