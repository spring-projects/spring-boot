/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.docs.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

/**
 * Example to show usage of {@link StreamsBuilder}.
 *
 * @author Stephane Nicoll
 */
public class KafkaStreamsBeanExample {

	// tag::configuration[]
	@Configuration(proxyBeanMethods = false)
	@EnableKafkaStreams
	public static class KafkaStreamsExampleConfiguration {

		@Bean
		public KStream<Integer, String> kStream(StreamsBuilder streamsBuilder) {
			KStream<Integer, String> stream = streamsBuilder.stream("ks1In");
			stream.map((k, v) -> new KeyValue<>(k, v.toUpperCase())).to("ks1Out",
					Produced.with(Serdes.Integer(), new JsonSerde<>()));
			return stream;
		}

	}
	// end::configuration[]

}
