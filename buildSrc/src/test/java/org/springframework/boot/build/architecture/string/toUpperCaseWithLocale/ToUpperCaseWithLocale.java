/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.string.toUpperCaseWithLocale;

import java.util.Locale;

class ToUpperCaseWithLocale {

	void exampleMethod() {
		String test = "Object must not be null";
		System.out.println(test.toUpperCase(Locale.ROOT));
	}

}
