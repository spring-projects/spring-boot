/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.caching

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Suppress("UNUSED_PARAMETER")
@Component
class MyMathService {

	@Cacheable("piDecimals")
	fun computePiDecimal(precision: Int): Int {
		/**/ return 0
	}

}

