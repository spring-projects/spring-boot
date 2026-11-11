/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jetty.util;

import java.util.Arrays;

public final class StringUtil {

	private StringUtil() {
	}

	public static String repeat(char c, int length) {
		char[] chars = new char[length];
		Arrays.fill(chars, c);
		return new String(chars);
	}

}
