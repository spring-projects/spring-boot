/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.web.reactive.webflux.errorhandling

import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class MyErrorWebExceptionHandler(
	errorAttributes: ErrorAttributes?, resources: WebProperties.Resources?,
	applicationContext: ApplicationContext?
) :
	AbstractErrorWebExceptionHandler(errorAttributes, resources, applicationContext) {
	override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
		return RouterFunctions.route(
			{ request: ServerRequest ->
				acceptsXml(
					request
				)
			}
		) { request: ServerRequest? ->
			handleErrorAsXml(
				request
			)
		}
	}

	private fun acceptsXml(request: ServerRequest): Boolean {
		return request.headers().accept().contains(MediaType.APPLICATION_XML)
	}

	fun handleErrorAsXml(request: ServerRequest?): Mono<ServerResponse> {
		val builder = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
		// ... additional builder calls
		return builder.build()
	}
}
