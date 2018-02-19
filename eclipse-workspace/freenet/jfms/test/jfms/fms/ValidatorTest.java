package jfms.fms;

import org.junit.Assert;
import org.junit.Test;

public class ValidatorTest {
	@Test
	public void testValidateUuid() {
		final String u1 = "12345678-90AB-CDEF-1234-567890abcdef@abcdefghijklmnopqrstuvwxyz01234567890abcdef";
		Assert.assertTrue(Validator.isValidUuid(u1));

		final String u2 = "12345678-90AB-CDEF-1234-567890abcd\uC3A9f@abcdefghijklmnopqrstuvwxyz01234567890abcdef";
		Assert.assertFalse(Validator.isValidUuid(u2));
	}
}
