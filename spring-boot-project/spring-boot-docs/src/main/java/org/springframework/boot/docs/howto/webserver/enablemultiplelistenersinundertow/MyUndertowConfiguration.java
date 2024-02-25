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

package org.springframework.boot.docs.howto.webserver.enablemultiplelistenersinundertow;

import io.undertow.Undertow.Builder;

import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyUndertowConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyUndertowConfiguration {

	/**
	 * Customizes the Undertow servlet web server factory by adding a listener.
	 * @param factory the Undertow servlet web server factory to be customized
	 * @return the customized Undertow servlet web server factory
	 */
	@Bean
	public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowListenerCustomizer() {
		return (factory) -> factory.addBuilderCustomizers(this::addHttpListener);
	}

	/**
	 * Adds an HTTP listener to the given builder with the specified port and address.
	 * @param builder the builder to add the HTTP listener to
	 * @return the updated builder with the HTTP listener added
	 */
	private Builder addHttpListener(Builder builder) {
		return builder.addHttpListener(8080, "0.0.0.0");
	}

}
