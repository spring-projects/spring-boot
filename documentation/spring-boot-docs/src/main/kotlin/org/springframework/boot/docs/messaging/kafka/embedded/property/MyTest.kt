/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.embedded.property

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker

@SpringBootTest
object MyTest {

	// tag::code[]
	init {
		System.setProperty(EmbeddedKafkaBroker.BROKER_LIST_PROPERTY, "spring.kafka.bootstrap-servers")
	}
	// end::code[]

}

