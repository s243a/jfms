package jfms.fms.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import jfms.fms.Sanitizer;

import jfms.fms.Trust;
import jfms.fms.Validator;

public class TrustListParser {
	private static final Logger LOG = Logger.getLogger(TrustListParser.class.getName());

	public List<Trust> parse(InputStream is) {
		List<Trust> trustList = null;

		try {
			XMLStreamReader reader = Utils.createXMLStreamReader(is);

			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT) {
					if (reader.getLocalName().equals("TrustList")) {
						trustList = parseTrustList(reader);
					}
					break;
				}
			}
		} catch (XMLStreamException e) {
			LOG.log(Level.WARNING, "Failed to parse trust list", e);
		}

		if (trustList == null) {
			trustList = Collections.emptyList();
		}
		return trustList;
	}

	private List<Trust> parseTrustList(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		List<Trust> trustList = new ArrayList<>();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2 && reader.getLocalName().equals("Trust")) {
					Trust trust = parseTrust(reader);
					level--;

					if (trust != null) {
						trustList.add(trust);
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		return trustList;
	}

	private Trust parseTrust(XMLStreamReader reader) throws XMLStreamException {
		int level = 1;
		boolean isFms = false;
		Trust trust = new Trust();

		do {
			int event = reader.next();
			switch(event) {
			case XMLStreamConstants.START_ELEMENT:
				level++;
				if (level == 2) {
					switch (reader.getLocalName()) {
					case "Identity":
						final String ssk = reader.getElementText();
						if (Validator.isValidSsk(ssk)) {
							trust.setIdentity(ssk);
						} else {
							LOG.log(Level.WARNING, "invalid SSK in trust list");
						}
						level--;
						break;
					case "MessageTrustLevel":
						try {
							trust.setMessageTrustLevel(
									Integer.parseInt(reader.getElementText()));
						} catch (NumberFormatException e) {
							LOG.log(Level.WARNING, "Failed to parse message trust");
						}
						level--;
						break;
					case "TrustListTrustLevel":
						try {
							trust.setTrustListTrustLevel(
									Integer.parseInt(reader.getElementText()));
						} catch (NumberFormatException e) {
							LOG.log(Level.WARNING, "Failed to parse trustlist trust");
						}
						level--;
						break;
					case "MessageTrustComment":
						trust.setMessageTrustComment(Sanitizer
								.sanitizeTrustComment(reader.getElementText()));
						level--;
						break;
					case "TrustListTrustComment":
						trust.setTrustListTrustComment(Sanitizer
								.sanitizeTrustComment(reader.getElementText()));
						level--;
						break;
					case "IsFMS":
						isFms = reader.getElementText().equals("true");
						level--;
						break;
					// ignore IsWOT because Web of Trust is not supported
					}
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				level--;
				break;
			}
		} while (level > 0);

		if (!isFms) {
			return null;
		}

		boolean valid = trust.getIdentity() != null;
		if (!valid) {
			LOG.log(Level.WARNING, "skipping invalid trust in trustlist");
			return null;
		}

		return trust;
	}
}
