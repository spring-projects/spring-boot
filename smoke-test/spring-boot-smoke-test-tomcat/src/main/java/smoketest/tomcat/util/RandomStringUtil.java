/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.util;

import java.util.Base64;
import java.util.Random;

public final class RandomStringUtil {

	private RandomStringUtil() {
	}

	public static String getRandomBase64EncodedString(int length) {
		byte[] responseHeader = new byte[length];
		new Random().nextBytes(responseHeader);
		return Base64.getEncoder().encodeToString(responseHeader);
	}

}
