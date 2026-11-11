/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.string.toLowerCaseWithLocale;

import java.util.Locale;

class ToLowerCaseWithLocale {

	void exampleMethod() {
		String test = "Object must not be null";
		System.out.println(test.toLowerCase(Locale.ENGLISH));
	}

}
