package jfms.fms;

import org.junit.Assert;
import org.junit.Test;

public class SanitizerTest {
	@Test
	public void testNameContainsCntrl() {
		final String expected = "abcdef";

		final String n1 = "abc\u0000def";
		Assert.assertEquals(expected, Sanitizer.sanitizeName(n1));

		final String n2 = "abc\u001Fdef";
		Assert.assertEquals(expected, Sanitizer.sanitizeName(n2));
	}

	@Test
	public void testMaxNameLength() {
		final String expected = "0123456789abcdefghijklmonpqrstuvwxyz0123";

		final String n1 = expected;
		Assert.assertEquals(expected, Sanitizer.sanitizeName(n1));

		final String n2 = "0123456789abcdefghijklmonpqrstuvwxyz01234";
		Assert.assertEquals(expected, Sanitizer.sanitizeName(n2));


		final String expected_utf8 = "\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A\u263A";

		final String n3 = expected_utf8;
		Assert.assertEquals(expected_utf8, Sanitizer.sanitizeName(n3));

		final String n4 = expected_utf8 + '\u263A';
		Assert.assertEquals(expected_utf8, Sanitizer.sanitizeName(n4));
	}

	@Test
	public void testNameEmpty() {
		final String expected = "";

		final String n1 = "";
		Assert.assertEquals(expected, Sanitizer.sanitizeName(n1));

		final String n2 = "\u0000";
		Assert.assertEquals(expected, Sanitizer.sanitizeName(n2));
	}

	@Test
	public void testBoardContainsCntrl() {
		final String expected = "abcdef";

		final String b1 = "abc\u0000def";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b1));

		final String b2 = "abc\u001Fdef";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b2));
	}

	@Test
	public void testBoardContainsComma() {
		final String expected = "abcdef";

		final String b1 = "abcdef,";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b1));

		final String b2 = "abcdef, ";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b2));

		final String b3 = "abcdef,123";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b3));
	}

	@Test
	public void testBoardContainsWhitespace() {
		final String b1 = " abcdef";
		Assert.assertEquals("abcdef", Sanitizer.sanitizeBoard(b1));

		final String b2 = "  abcdef";
		Assert.assertEquals("abcdef", Sanitizer.sanitizeBoard(b2));

		final String b3 = "abcdef ";
		Assert.assertEquals("abcdef_", Sanitizer.sanitizeBoard(b3));

		final String b4 = "abcdef  ";
		Assert.assertEquals("abcdef__", Sanitizer.sanitizeBoard(b4));

		final String b5 = "abc_def";
		Assert.assertEquals("abc_def", Sanitizer.sanitizeBoard(b5));

		final String b6 = "abc\u3000def";
		Assert.assertEquals("abc_def", Sanitizer.sanitizeBoard(b6));

		final String b7 = "abc\u2029def";
		Assert.assertEquals("abc_def", Sanitizer.sanitizeBoard(b7));
	}

	@Test
	public void testMaxBoardLength() {
		final String expected = "0123456789abcdefghijklmonpqrstuvwxyz0123";

		final String b1 = expected;
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b1));

		final String b2 = "0123456789abcdefghijklmonpqrstuvwxyz01234";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b2));


		final String expected_utf8 = "\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7\u56E7";

		final String b3 = expected_utf8;
		Assert.assertEquals(expected_utf8, Sanitizer.sanitizeBoard(b3));

		final String b4 = expected_utf8 + '\u56E7';
		Assert.assertEquals(expected_utf8, Sanitizer.sanitizeBoard(b4));
	}

	@Test
	public void testBoardContainsUppercase() {
		final String expected = "abcdef";

		final String b1 = "Abcdef";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b1));

		final String b2 = "abcdeF";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b2));

		final String b3 = "ABCDEF";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b3));
	}

	@Test
	public void testBoardEmpty() {
		final String expected = "";

		final String b1 = "";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b1));

		final String b2 = "\u0000";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b2));

		final String b3 = ",abc";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b3));

		final String b4 = " ";
		Assert.assertEquals(expected, Sanitizer.sanitizeBoard(b4));
	}
}
