/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.registeringcustom

internal class Dictionary {

	val words: List<String>
		get() = emptyList()

	companion object {
		fun load(): Dictionary {
			return Dictionary()
		}
	}

}

