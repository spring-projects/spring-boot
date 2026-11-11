/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.embedded.annotation

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka

@SpringBootTest
@EmbeddedKafka(topics = ["someTopic"], bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class MyTest {

	// ...

}

