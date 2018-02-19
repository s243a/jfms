package jfms.fms.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Utils {
	public static XMLStreamReader createXMLStreamReader(InputStream stream)
		throws XMLStreamException {

		return createInputFactory().createXMLStreamReader(stream);
	}

	public static XMLStreamReader createXMLStreamReader(Reader reader)
		throws XMLStreamException {

		return createInputFactory().createXMLStreamReader(reader);
	}

	public static XMLStreamWriter createXMLStreamWriter(OutputStream stream,
			String encoding) throws XMLStreamException {

		final XMLOutputFactory xof = XMLOutputFactory.newInstance();
		return xof.createXMLStreamWriter(stream, encoding);
	}

	public static XMLStreamWriter createXMLStreamWriter(Writer stream)
		throws XMLStreamException {

		final XMLOutputFactory xof = XMLOutputFactory.newInstance();
		return xof.createXMLStreamWriter(stream);
	}

	private static XMLInputFactory createInputFactory() {
		final XMLInputFactory xmlif = XMLInputFactory.newInstance();
		xmlif.setProperty(
				XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
				Boolean.FALSE);
		xmlif.setProperty(
				XMLInputFactory.SUPPORT_DTD,
				Boolean.FALSE);

		return xmlif;
	}
}
