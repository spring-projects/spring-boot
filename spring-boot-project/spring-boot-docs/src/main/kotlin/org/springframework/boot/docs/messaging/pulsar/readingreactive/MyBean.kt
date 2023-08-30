/*
* Copyright 2023-2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.springframework.boot.docs.messaging.pulsar.readingreactive

import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderBuilder
import org.apache.pulsar.reactive.client.api.StartAtSpec
import org.springframework.pulsar.reactive.core.ReactiveMessageReaderBuilderCustomizer
import org.springframework.pulsar.reactive.core.ReactivePulsarReaderFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
@Component
class MyBean(private val pulsarReaderFactory: ReactivePulsarReaderFactory<String>) {

	fun someMethod() {
		val readerBuilderCustomizer = ReactiveMessageReaderBuilderCustomizer {
			readerBuilder: ReactiveMessageReaderBuilder<String> ->
				readerBuilder
					.topic("someTopic")
					.startAtSpec(StartAtSpec.ofInstant(Instant.now().minusSeconds(5)))
		}
		val message = pulsarReaderFactory
				.createReader(Schema.STRING, listOf(readerBuilderCustomizer))
				.readOne()
		// ...
	}

}
