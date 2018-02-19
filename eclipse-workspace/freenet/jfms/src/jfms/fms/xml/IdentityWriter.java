package jfms.fms.xml;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jfms.fms.Identity;
import jfms.fms.Sanitizer;

public class IdentityWriter {
	private static final Logger LOG = Logger.getLogger(IdentityWriter.class.getName());

	public byte[] writeXml(Identity identity) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			XMLStreamWriter xtw = Utils.createXMLStreamWriter(bos, "UTF-8");

			xtw.writeStartDocument("UTF-8", "1.0");
			xtw.writeStartElement("Identity");

			xtw.writeStartElement("Name");
			xtw.writeCharacters(Sanitizer.sanitizeName(identity.getName()));
			xtw.writeEndElement();

			if (identity.getSignature() != null) {
				xtw.writeStartElement("Signature");
				xtw.writeCharacters(identity.getSignature());
				xtw.writeEndElement();
			}

			if (identity.getAvatar() != null) {
				xtw.writeStartElement("Avatar");
				xtw.writeCharacters(identity.getAvatar());
				xtw.writeEndElement();
			}

			xtw.writeStartElement("SingleUse");
			xtw.writeCharacters(Boolean.toString(identity.getSingleUse()));
			xtw.writeEndElement();

			xtw.writeStartElement("PublishTrustList");
			xtw.writeCharacters(Boolean.toString(identity.getPublishTrustList()));
			xtw.writeEndElement();

			xtw.writeStartElement("PublishBoardList");
			xtw.writeCharacters(Boolean.toString(identity.getPublishBoardList()));
			xtw.writeEndElement();

			xtw.writeEndElement();
			xtw.writeEndDocument();

			xtw.flush();
			xtw.close();
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to create Identity XML", e);
		}

		return bos.toByteArray();
	}
}
