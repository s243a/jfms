package jfms.fms.xml;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jfms.fms.Identity;
import jfms.fms.Sanitizer;
import jfms.fms.Validator;

public class IdentityParser {
	private static final Logger LOG = Logger.getLogger(IdentityParser.class.getName());

	public Identity parse(InputStream is) {
		Identity identity = null;

		try {
			XMLStreamReader reader = Utils.createXMLStreamReader(is);

			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT) {
					if (reader.getLocalName().equals("Identity")) {
						identity = parseIdentity(reader);
					}
					break;
				}
			}
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to parse identity", e);
		}

		return identity;
	}

	private Identity parseIdentity(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		Identity identity = new Identity();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					switch (reader.getLocalName()) {
					case "Name":
						identity.setName(Sanitizer.sanitizeName(
								reader.getElementText()));
						level--;
						break;
					case "Signature":
						identity.setSignature(reader.getElementText());
						level--;
						break;
					case "Avatar":
						final String avatar = reader.getElementText();
						if (Validator.isValidFreenetURI(avatar)) {
							identity.setSignature(avatar);
						} else {
							LOG.log(Level.WARNING, "failed to parse avatar");
						}
						level--;
						break;
					case "SingleUse":
						identity.setSingleUse(
								reader.getElementText().equals("true"));
						level--;
						break;
					case "PublishTrustList":
						identity.setPublishTrustList(
								reader.getElementText().equals("true"));
						level--;
						break;
					case "PublishBoardList":
						identity.setPublishBoardList(
								reader.getElementText().equals("true"));
						level--;
						break;
					case "FreesiteEdition":
						try {
							identity.setFreesiteEdition(
									Integer.parseInt(reader.getElementText()));
						} catch (NumberFormatException e) {
							LOG.log(Level.WARNING, "Failed to parse freesite edition");
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

		return identity;
	}
}
