/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.caching;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class MyMathService {

	@Cacheable("piDecimals")
	public int computePiDecimal(int precision) {
		/**/ return 0;
	}

}
