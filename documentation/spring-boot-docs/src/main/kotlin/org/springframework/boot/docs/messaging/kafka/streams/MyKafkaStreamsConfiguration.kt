/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.streams

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.support.serializer.JacksonJsonSerde

@Suppress("UNUSED_PARAMETER")
@Configuration(proxyBeanMethods = false)
@EnableKafkaStreams
class MyKafkaStreamsConfiguration {

	@Bean
	fun kStream(streamsBuilder: StreamsBuilder): KStream<Int, String> {
		val stream = streamsBuilder.stream<Int, String>("ks1In")
		stream.map(this::uppercaseValue).to("ks1Out", Produced.with(Serdes.Integer(), JacksonJsonSerde()))
		return stream
	}

	private fun uppercaseValue(key: Int, value: String): KeyValue<Int, String> {
		return KeyValue(key, value.uppercase())
	}

}

