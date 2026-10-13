/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.utilities.testpropertyvalues

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.mock.env.MockEnvironment

class MyEnvironmentTests {

	@Test
	fun testPropertySources() {
		val environment = MockEnvironment()
		TestPropertyValues.of("org=Spring", "name=Boot").applyTo(environment)
		assertThat(environment.getProperty("name")).isEqualTo("Boot")
	}

}

