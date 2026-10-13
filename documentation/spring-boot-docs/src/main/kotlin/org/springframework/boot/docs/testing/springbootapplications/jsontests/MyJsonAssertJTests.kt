/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.jsontests

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester

@JsonTest
class MyJsonAssertJTests(@Autowired val json: JacksonTester<SomeObject>) {

	// tag::code[]
	@Test
	fun someTest() {
		val value = SomeObject(0.152f)
		assertThat(json.write(value)).extractingJsonPathNumberValue("@.test.numberValue")
			.satisfies(ThrowingConsumer { number ->
				assertThat(number.toFloat()).isCloseTo(0.15f, within(0.01f))
			})
	}
	// end::code[]

}

