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

package org.springframework.boot.docs.context.embedded;

import org.apache.tomcat.util.http.LegacyCookieProcessor;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example configuration for configuring Tomcat with to use {@link LegacyCookieProcessor}.
 *
 * @author Andy Wilkinson
 */
public class TomcatLegacyCookieProcessorExample {

	/**
	 * Configuration class that declares the required {@link WebServerFactoryCustomizer}.
	 */
	@Configuration(proxyBeanMethods = false)
	public static class LegacyCookieProcessorConfiguration {

		// tag::customizer[]
		@Bean
		public WebServerFactoryCustomizer<TomcatServletWebServerFactory> cookieProcessorCustomizer() {
			return (factory) -> factory
					.addContextCustomizers((context) -> context.setCookieProcessor(new LegacyCookieProcessor()));
		}
		// end::customizer[]

	}

}
