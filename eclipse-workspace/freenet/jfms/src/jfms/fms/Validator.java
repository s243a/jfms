package jfms.fms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {
	private static final Pattern FREENET_URI_PATTERN = Pattern.compile(
			"(CHK|SSK|USK)@[\\p{Alnum}~-]{43},[\\p{Alnum}~-]{43},[\\p{Alnum}~-]{7}.*");
	private static final Pattern SSK_PATTERN = Pattern.compile(
			"SSK@[\\p{Alnum}~-]{43,44},[\\p{Alnum}~-]{43},[\\p{Alnum}~-]{7}/");
	private static final Pattern UUID_PATTERN = Pattern.compile(
			"\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}@\\p{Alnum}{0,43}");

	public static final boolean isValidFreenetURI(String uri) {
		// TODO check path part, support KSK
		Matcher m = FREENET_URI_PATTERN.matcher(uri);
		return m.matches();
	}

	public static final boolean isValidSsk(String ssk) {
		Matcher m = SSK_PATTERN.matcher(ssk);
		return m.matches();
	}

	public static final boolean isValidUuid(String uuid) {
		// File Formats specification:
		// All UUIDs should follow the UUID standard, but failing that, they
		// MUST be unique and MUST NOT contain the | character. The only
		// characters a UUID in FMS may contain are A-Z, a-z, 0-9, @, _, -,
		// and ~
		//
		// The ID of the message should begin with a standard UUID then an @
		// and then the first part of the inserting identity's SSK (the part
		// between the @ and first ,) with the ~ and - removed.

		Matcher m = UUID_PATTERN.matcher(uuid);
		return m.matches();
	}
}
