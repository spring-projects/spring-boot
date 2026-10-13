/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.webflux.httpcodecs

import org.springframework.boot.http.codec.CodecCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.http.codec.CodecConfigurer
import org.springframework.http.codec.ServerSentEventHttpMessageReader

class MyCodecsConfiguration {

	@Bean
	fun myCodecCustomizer(): CodecCustomizer {
		return CodecCustomizer { configurer: CodecConfigurer ->
			configurer.registerDefaults(false)
			configurer.customCodecs().register(ServerSentEventHttpMessageReader())
		}
	}

}

