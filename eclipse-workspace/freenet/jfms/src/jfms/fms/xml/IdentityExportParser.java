package jfms.fms.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jfms.fms.LocalIdentity;
import jfms.fms.Sanitizer;
import jfms.fms.Validator;

public class IdentityExportParser {
	private static final Logger LOG = Logger.getLogger(IdentityExportParser.class.getName());

	public List<LocalIdentity> parse(InputStream is) {
		List<LocalIdentity> identities = null;

		try {
			XMLStreamReader reader = Utils.createXMLStreamReader(is);

			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT) {
					if (reader.getLocalName().equals("IdentityExport")) {
						identities = parseIdentityExport(reader);
					}
					break;
				}
			}
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to parse identity export", e);
		}

		return identities;
	}

	private List<LocalIdentity> parseIdentityExport(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		List<LocalIdentity> identities = new ArrayList<>();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2 && reader.getLocalName().equals("Identity")) {
					LocalIdentity identity = parseIdentity(reader);
					if (identity != null) {
						identities.add(identity);
					}
					level--;
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		return identities;
	}

	private LocalIdentity parseIdentity(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		LocalIdentity identity = new LocalIdentity();

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
					case "PublicKey":
						final String ssk = reader.getElementText();
						if (Validator.isValidSsk(ssk)) {
							identity.setSsk(ssk);
						} else {
							LOG.log(Level.WARNING, "invalid SSK in identity XML");
						}
						level--;
						break;
					case "PrivateKey":
						final String privateSsk = reader.getElementText();
						if (Validator.isValidSsk(privateSsk)) {
							identity.setPrivateSsk(privateSsk);
						} else {
							LOG.log(Level.WARNING, "invalid private SSK in identity XML");
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
					case "PublishFreesite":
						// TODO
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
			identity.getSsk() != null &&
			identity.getPrivateSsk() != null &&
			identity.getName() != null && !identity.getName().isEmpty();

		if (!valid) {
			LOG.log(Level.WARNING, "skipping invalid identity");
			return null;
		}

		return identity;
	}
}
