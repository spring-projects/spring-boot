/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.reactive.context;

import org.junit.Test;

import org.springframework.boot.web.reactive.context.config.ExampleReactiveWebServerApplicationConfiguration;
import org.springframework.boot.web.reactive.server.MockReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationConfigReactiveWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
public class AnnotationConfigReactiveWebServerApplicationContextTests {

	private AnnotationConfigReactiveWebServerApplicationContext context;

	@Test
	public void createFromScan() throws Exception {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				ExampleReactiveWebServerApplicationConfiguration.class.getPackage()
						.getName());
		verifyContext();
	}

	@Test
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				ExampleReactiveWebServerApplicationConfiguration.class);
		verifyContext();
	}

	@Test
	public void registerAndRefresh() throws Exception {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext();
		this.context.register(ExampleReactiveWebServerApplicationConfiguration.class);
		this.context.refresh();
		verifyContext();
	}

	@Test
	public void scanAndRefresh() throws Exception {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext();
		this.context.scan(ExampleReactiveWebServerApplicationConfiguration.class
				.getPackage().getName());
		this.context.refresh();
		verifyContext();
	}

	private void verifyContext() {
		MockReactiveWebServerFactory factory = this.context
				.getBean(MockReactiveWebServerFactory.class);
		HttpHandler httpHandler = this.context.getBean(HttpHandler.class);
		assertThat(factory.getWebServer().getHttpHandler()).isEqualTo(httpHandler);
	}

	@Configuration
	public static class WebServerConfiguration {

		@Bean
		public ReactiveWebServerFactory webServerFactory() {
			return new MockReactiveWebServerFactory();
		}

	}

}
