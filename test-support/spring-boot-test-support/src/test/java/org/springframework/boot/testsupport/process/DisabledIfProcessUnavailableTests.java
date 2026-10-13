/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.process;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link DisabledIfProcessUnavailable}.
 *
 * @author Phillip Webb
 */
@DisabledIfProcessUnavailable("iverymuchdontexist")
class DisabledIfProcessUnavailableTests {

	@Test
	void test() {
		fail("I should have been disabled");
	}

}
