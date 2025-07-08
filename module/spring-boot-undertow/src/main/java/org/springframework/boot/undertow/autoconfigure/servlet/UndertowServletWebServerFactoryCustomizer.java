/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.undertow.autoconfigure.servlet;

import org.springframework.boot.undertow.autoconfigure.UndertowServerProperties;
import org.springframework.boot.undertow.servlet.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to Undertow
 * Servlet web servers.
 *
 * @author Andy Wilkinson
 */
class UndertowServletWebServerFactoryCustomizer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

	private final UndertowServerProperties undertowProperties;

	UndertowServletWebServerFactoryCustomizer(UndertowServerProperties undertowProperties) {
		this.undertowProperties = undertowProperties;
	}

	@Override
	public void customize(UndertowServletWebServerFactory factory) {
		factory.setEagerFilterInit(this.undertowProperties.isEagerFilterInit());
		factory.setPreservePathOnForward(this.undertowProperties.isPreservePathOnForward());
	}

}
