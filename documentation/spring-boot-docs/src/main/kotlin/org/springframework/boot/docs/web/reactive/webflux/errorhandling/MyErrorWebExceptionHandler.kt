/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.webflux.errorhandling

import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.webflux.error.ErrorAttributes
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Suppress("UNUSED_PARAMETER")
@Component
class MyErrorWebExceptionHandler(
		errorAttributes: ErrorAttributes, webProperties: WebProperties,
		applicationContext: ApplicationContext, serverCodecConfigurer: ServerCodecConfigurer
) : AbstractErrorWebExceptionHandler(errorAttributes, webProperties.resources, applicationContext) {

	init {
		setMessageReaders(serverCodecConfigurer.readers)
		setMessageWriters(serverCodecConfigurer.writers)
	}

	override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
		return RouterFunctions.route(this::acceptsXml, this::handleErrorAsXml)
	}

	private fun acceptsXml(request: ServerRequest): Boolean {
		return request.headers().accept().contains(MediaType.APPLICATION_XML)
	}

	fun handleErrorAsXml(request: ServerRequest): Mono<ServerResponse> {
		val builder = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
		// ... additional builder calls
		return builder.build()
	}

}

