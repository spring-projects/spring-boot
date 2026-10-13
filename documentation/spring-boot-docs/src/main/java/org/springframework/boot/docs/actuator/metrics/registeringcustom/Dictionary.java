/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.registeringcustom;

import java.util.Collections;
import java.util.List;

class Dictionary {

	static Dictionary load() {
		return new Dictionary();
	}

	List<String> getWords() {
		return Collections.emptyList();
	}

}
