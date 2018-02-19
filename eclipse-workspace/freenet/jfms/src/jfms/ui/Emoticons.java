package jfms.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jfms.config.Config;

public class Emoticons {
	public static final int FACE_ANGEL       =  0;
	public static final int FACE_EMBARRASSED =  1;
	public static final int FACE_KISS        =  2;
	public static final int FACE_LAUGH       =  3;
	public static final int FACE_PLAIN       =  4;
	public static final int FACE_RASPBERRY   =  5;
	public static final int FACE_SAD         =  6;
	public static final int FACE_SMILE_BIG   =  7;
	public static final int FACE_SMILE       =  8;
	public static final int FACE_SURPRISE    =  9;
	public static final int FACE_UNCERTAIN   = 10;
	public static final int FACE_WINK        = 11;
	public static final int FACE_MAX         = 12;

	private static final Logger LOG = Logger.getLogger(Emoticons.class.getName());

	private static final String[] icons = new String[] {
		"0:-)", // face-angel
		":-[",  // face-embarrassed
		":-*",  // face-kiss
		":-))", // face-laugh
		":-|",  // face-plain
		":-P",  // face-raspberry
		":-(",  // face-sad
		":(",   // face-sad
		":-D",  // face-smile-big
		":-)",  // face-smile
		":)",   // face-smile
		":-0",  // face-surprise
		":-/",  // face-uncertain
		";-)",  // face-wink
		";)"    // face-wink
	};

	private static final String[] names = new String[] {
		"face-angel.png",
		"face-embarrassed.png",
		"face-kiss.png",
		"face-laugh.png",
		"face-plain.png",
		"face-raspberry.png",
		"face-sad.png",
		"face-sad.png",
		"face-smile-big.png",
		"face-smile.png",
		"face-smile.png",
		"face-surprise.png",
		"face-uncertain.png",
		"face-wink.png",
		"face-wink.png"
	};

	private static Emoticons instance;

	private Pattern pattern;
	private String[] imgTags;


	public static synchronized Emoticons getInstance() {
		if (instance == null) {
			instance = new Emoticons();
		}

		return instance;
	}

	public static byte[] getImageData(String theme, int type) {
		final String path = "/icons/" + theme + "/" + names[type];

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (InputStream is = Emoticons.class.getResourceAsStream(path)) {
			if (is == null) {
				LOG.log(Level.WARNING, "Failed to load emoticon from {0}", path);
				return null;
			}
			byte[] buf = new byte[1024];

			int len;
			while ((len = is.read(buf)) > 0) {
				bos.write(buf, 0, len);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to load emoticon from " + path, e);
			return null;
		}

		return bos.toByteArray();
	}


	private Emoticons() {
		final String theme = Config.getInstance().getIconSet();

		StringBuilder regex = new StringBuilder();
		regex.append('(');
		for (String i : icons) {
			regex.append(Pattern.quote(i));
			regex.append('|');
		}
		regex.setCharAt(regex.length()-1, ')');
		pattern = Pattern.compile(regex.toString());

		imgTags = new String[icons.length];

		Base64.Encoder b64Enc = Base64.getEncoder();
		for (int i=0; i<icons.length; i++) {
			byte[] data = getImageData(theme, i);
			if (data == null) {
				continue;
			}

			StringBuilder str= new StringBuilder();
			str.append("<img src=\"data:image/png;base64,");
			str.append(b64Enc.encodeToString(data));
			str.append("\" alt=\"");
			str.append(icons[i]);
			str.append("\" title=\"");
			str.append(icons[i]);
			str.append("\">");

			imgTags[i] = str.toString();
		}
	}

	public final String replaceByImgTags(String input) {
		StringBuffer output = new StringBuffer();
		Matcher m = pattern.matcher(input);

		int emoticonCount = 0;

		while (m.find()) {
			String group = m.group();

			int codePoint = 0;
			if (m.start() > 0) {
				codePoint = input.codePointAt(m.start()-1);
			}

			// only allow emoticons after '>' and whitespace
			// the former case catches emoticons directly after HTML tags
			if (codePoint != 0x3e && !Character.isWhitespace(codePoint)) {
				continue;
			}

			for (int i=0; i<icons.length; i++ ) {
				if (group.equals(icons[i])) {
					if (imgTags[i] != null) {
						m.appendReplacement(output, imgTags[i]);
						emoticonCount++;
					}
					break;
				}
			}

			// limit maximum number of emoticons/messages
			// to avoid huge HTML
			if (emoticonCount >= 100) {
				break;
			}
		}
		m.appendTail(output);

		return output.toString();
	}
}
