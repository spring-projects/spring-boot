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

package org.springframework.boot.docs.messaging.pulsar.readingreactive;

import java.time.Instant;
import java.util.List;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.reactive.client.api.StartAtSpec;
import reactor.core.publisher.Mono;

import org.springframework.pulsar.reactive.core.ReactiveMessageReaderBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactivePulsarReaderFactory;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final ReactivePulsarReaderFactory<String> pulsarReaderFactory;

	public MyBean(ReactivePulsarReaderFactory<String> pulsarReaderFactory) {
		this.pulsarReaderFactory = pulsarReaderFactory;
	}

	public void someMethod() {
		ReactiveMessageReaderBuilderCustomizer<String> readerBuilderCustomizer = (readerBuilder) -> readerBuilder
			.topic("someTopic")
			.startAtSpec(StartAtSpec.ofInstant(Instant.now().minusSeconds(5)));
		Mono<Message<String>> message = this.pulsarReaderFactory
			.createReader(Schema.STRING, List.of(readerBuilderCustomizer))
			.readOne();
		// ...
	}

}
