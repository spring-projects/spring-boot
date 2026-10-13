/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.caching.testing;

import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureCache
public class MyIntegrationTests {

	// Tests use a no-op cache manager

}
