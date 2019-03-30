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

package org.springframework.boot.web.reactive.context.config;

import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContextTests;
import org.springframework.boot.web.reactive.server.MockReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import static org.mockito.Mockito.mock;

/**
 * Example {@code @Configuration} for use with
 * {@link AnnotationConfigReactiveWebServerApplicationContextTests}.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
public class ExampleReactiveWebServerApplicationConfiguration {

	@Bean
	public MockReactiveWebServerFactory webServerFactory() {
		return new MockReactiveWebServerFactory();
	}

	@Bean
	public HttpHandler httpHandler() {
		return mock(HttpHandler.class);
	}

}
