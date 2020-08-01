/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.reactive.context.WebServerManager.DelayedInitializationHttpHandler;
import org.springframework.boot.web.reactive.context.config.ExampleReactiveWebServerApplicationConfiguration;
import org.springframework.boot.web.reactive.server.MockReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AnnotationConfigReactiveWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class AnnotationConfigReactiveWebServerApplicationContextTests {

	private AnnotationConfigReactiveWebServerApplicationContext context;

	@Test
	void createFromScan() {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				ExampleReactiveWebServerApplicationConfiguration.class.getPackage().getName());
		verifyContext();
	}

	@Test
	void createFromConfigClass() {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				ExampleReactiveWebServerApplicationConfiguration.class);
		verifyContext();
	}

	@Test
	void registerAndRefresh() {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext();
		this.context.register(ExampleReactiveWebServerApplicationConfiguration.class);
		this.context.refresh();
		verifyContext();
	}

	@Test
	void multipleRegistersAndRefresh() {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext();
		this.context.register(WebServerConfiguration.class);
		this.context.register(HttpHandlerConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(WebServerConfiguration.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(HttpHandlerConfiguration.class)).hasSize(1);
	}

	@Test
	void scanAndRefresh() {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext();
		this.context.scan(ExampleReactiveWebServerApplicationConfiguration.class.getPackage().getName());
		this.context.refresh();
		verifyContext();
	}

	@Test
	void httpHandlerInitialization() {
		// gh-14666
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(InitializationTestConfig.class);
		verifyContext();
	}

	private void verifyContext() {
		MockReactiveWebServerFactory factory = this.context.getBean(MockReactiveWebServerFactory.class);
		HttpHandler expectedHandler = this.context.getBean(HttpHandler.class);
		HttpHandler actualHandler = factory.getWebServer().getHttpHandler();
		if (actualHandler instanceof DelayedInitializationHttpHandler) {
			actualHandler = ((DelayedInitializationHttpHandler) actualHandler).getHandler();
		}
		assertThat(actualHandler).isEqualTo(expectedHandler);
	}

	@Configuration(proxyBeanMethods = false)
	static class WebServerConfiguration {

		@Bean
		ReactiveWebServerFactory webServerFactory() {
			return new MockReactiveWebServerFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpHandlerConfiguration {

		@Bean
		HttpHandler httpHandler() {
			return mock(HttpHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class InitializationTestConfig {

		private static boolean addedListener;

		@Bean
		ReactiveWebServerFactory webServerFactory() {
			return new MockReactiveWebServerFactory();
		}

		@Bean
		HttpHandler httpHandler() {
			if (!addedListener) {
				throw new RuntimeException(
						"Handlers should be added after listeners, we're being initialized too early!");
			}
			return mock(HttpHandler.class);
		}

		@Bean
		Listener listener() {
			return new Listener();
		}

		@Bean
		ApplicationEventMulticaster applicationEventMulticaster() {
			return new SimpleApplicationEventMulticaster() {

				@Override
				public void addApplicationListenerBean(String listenerBeanName) {
					super.addApplicationListenerBean(listenerBeanName);
					if ("listener".equals(listenerBeanName)) {
						addedListener = true;
					}
				}

			};
		}

		static class Listener implements ApplicationListener<ContextRefreshedEvent> {

			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {
			}

		}

	}

}
