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

package org.springframework.boot.test.autoconfigure.hateoas;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebTestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebHandler;

/**
 * Tests for hypermedia-based {@link WebTestClient}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Greg Turnquist
 */
class HypermediaTestAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebTestClientAutoConfiguration.class));

	@Test
	void codecsCustomizerShouldRegisterHypermediaTypesWithWebTestClient() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);
			assertWebTestClientHasHypermedia(webTestClient, MediaTypes.HAL_JSON);
		});
	}

	@Test
	void codecsCustomizerShouldRegisterAlternativeHypermediaTypesWithWebTestClient() {
		this.contextRunner.withUserConfiguration(HalFormsConfiguration.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);
			assertWebTestClientHasHypermedia(webTestClient, MediaTypes.HAL_FORMS_JSON);
		});
	}

	@Test
	void codecsCustomizerShouldRegisterAllHypermediaTypesWithWebTestClient() {
		this.contextRunner.withUserConfiguration(AllHypermediaTypesConfiguration.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);
			assertWebTestClientHasHypermedia(webTestClient, MediaTypes.HAL_JSON, MediaTypes.HAL_FORMS_JSON,
					MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
		});
	}

	private static void assertWebTestClientHasHypermedia(WebTestClient webTestClient, MediaType... mediaTypes) {

		WebClient webClient = (WebClient) ReflectionTestUtils.getField(webTestClient, "webClient");
		ExchangeStrategies strategies = (ExchangeStrategies) ReflectionTestUtils
				.getField(ReflectionTestUtils.getField(webClient, "exchangeFunction"), "strategies");

		assertThat(strategies.messageReaders()).flatExtracting(HttpMessageReader::getReadableMediaTypes)
				.contains(mediaTypes);
		assertThat(strategies.messageWriters()).flatExtracting(HttpMessageWriter::getWritableMediaTypes)
				.contains(mediaTypes);
	}

	@ImportAutoConfiguration(HypermediaTestAutoConfiguration.class)
	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class BaseConfiguration {

		@Bean
		WebHandler webHandler() {
			return mock(WebHandler.class);
		}
	}

	@ImportAutoConfiguration(HypermediaTestAutoConfiguration.class)
	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL_FORMS)
	static class HalFormsConfiguration {

		@Bean
		WebHandler webHandler() {
			return mock(WebHandler.class);
		}
	}

	@ImportAutoConfiguration(HypermediaTestAutoConfiguration.class)
	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = {HypermediaType.HAL, HypermediaType.HAL_FORMS, HypermediaType.COLLECTION_JSON,
			HypermediaType.UBER})
	static class AllHypermediaTypesConfiguration {

		@Bean
		WebHandler webHandler() {
			return mock(WebHandler.class);
		}
	}
}
