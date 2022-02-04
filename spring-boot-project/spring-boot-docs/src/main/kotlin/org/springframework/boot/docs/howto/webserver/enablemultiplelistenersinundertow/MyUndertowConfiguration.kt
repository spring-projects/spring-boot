/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.howto.webserver.enablemultiplelistenersinundertow

import io.undertow.Undertow
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyUndertowConfiguration {

	@Bean
	fun undertowListenerCustomizer(): WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
		return WebServerFactoryCustomizer { factory: UndertowServletWebServerFactory ->
			factory.addBuilderCustomizers(
				UndertowBuilderCustomizer { builder: Undertow.Builder -> addHttpListener(builder) })
		}
	}

	private fun addHttpListener(builder: Undertow.Builder): Undertow.Builder {
		return builder.addHttpListener(8080, "0.0.0.0")
	}

}
