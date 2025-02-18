/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.boot.docs.actuator.cloudfoundry.customcontextpath

import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.reactive.ContextPathCompositeHandler
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.core.publisher.Mono

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebFluxProperties::class)
class MyReactiveCloudFoundryConfiguration {

	@Bean
	fun httpHandler(applicationContext: ApplicationContext, properties: WebFluxProperties): HttpHandler {
		val httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build()
		return CloudFoundryHttpHandler(properties.basePath, httpHandler)
	}

	private class CloudFoundryHttpHandler(basePath: String, private val delegate: HttpHandler) : HttpHandler {
		private val contextPathDelegate = ContextPathCompositeHandler(mapOf(basePath to delegate))

		override fun handle(request: ServerHttpRequest, response: ServerHttpResponse): Mono<Void> {
			// Remove underlying context path first (e.g. Servlet container)
			val path = request.path.pathWithinApplication().value()
			return if (path.startsWith("/cloudfoundryapplication")) {
				delegate.handle(request, response)
			} else {
				contextPathDelegate.handle(request, response)
			}
		}
	}
}
