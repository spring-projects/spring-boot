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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebTestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for hypermedia-based {@link WebTestClient}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Greg Turnquist
 */
class HypermediaWebTestClientAutoConfigurationTests {

	static MediaType FRODO_JSON = MediaType.parseMediaType("application/frodo+json");

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withUserConfiguration(BaseConfiguration.class);

	@Test
	void codecsCustomizerShouldRegisterHypermediaTypesWithWebTestClient() {
		this.contextRunner.withUserConfiguration(HalConfig.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes).contains(MediaTypes.HAL_JSON)
					.doesNotContain(MediaTypes.HAL_FORMS_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
			assertThat(exchangeStrategies(webTestClient).messageWriters())
					.flatExtracting(HttpMessageWriter::getWritableMediaTypes).contains(MediaTypes.HAL_JSON)
					.doesNotContain(MediaTypes.HAL_FORMS_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
		});
	}

	@Test
	void codecsCustomizerShouldRegisterAlternativeHypermediaTypesWithWebTestClient() {
		this.contextRunner.withUserConfiguration(HalFormsConfig.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes).contains(MediaTypes.HAL_FORMS_JSON)
					.doesNotContain(MediaTypes.HAL_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
			assertThat(exchangeStrategies(webTestClient).messageWriters())
					.flatExtracting(HttpMessageWriter::getWritableMediaTypes).contains(MediaTypes.HAL_FORMS_JSON)
					.doesNotContain(MediaTypes.HAL_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
		});
	}

	@Test
	void codecsCustomizerShouldRegisterAllHypermediaTypesWithWebTestClient() {
		this.contextRunner.withUserConfiguration(AllHypermediaConfig.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes).contains(MediaTypes.HAL_JSON,
							MediaTypes.HAL_FORMS_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
			assertThat(exchangeStrategies(webTestClient).messageWriters())
					.flatExtracting(HttpMessageWriter::getWritableMediaTypes).contains(MediaTypes.HAL_JSON,
							MediaTypes.HAL_FORMS_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
		});
	}

	@Test
	void codecsCustomizerShouldRegisterCustomHypermediaTypesWithWebTestClient() {
		this.contextRunner.withUserConfiguration(CustomHypermediaConfig.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes).contains(MediaTypes.HAL_JSON, FRODO_JSON)
					.doesNotContain(MediaTypes.HAL_FORMS_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
			assertThat(exchangeStrategies(webTestClient).messageWriters())
					.flatExtracting(HttpMessageWriter::getWritableMediaTypes).contains(MediaTypes.HAL_JSON, FRODO_JSON)
					.doesNotContain(MediaTypes.HAL_FORMS_JSON, MediaTypes.COLLECTION_JSON, MediaTypes.UBER_JSON);
		});
	}

	/**
	 * Extract the {@link ExchangeStrategies} from a {@link WebTestClient} to assert it
	 * has the proper message readers and writers.
	 * @param webTestClient
	 * @return
	 */
	private static ExchangeStrategies exchangeStrategies(WebTestClient webTestClient) {
		WebClient webClient = (WebClient) ReflectionTestUtils.getField(webTestClient, "webClient");
		return (ExchangeStrategies) ReflectionTestUtils
				.getField(ReflectionTestUtils.getField(webClient, "exchangeFunction"), "strategies");
	}

	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class, WebTestClientAutoConfiguration.class })
	@EnableWebFlux
	static class BaseConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class HalConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL_FORMS)
	static class HalFormsConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = { HypermediaType.HAL, HypermediaType.HAL_FORMS, HypermediaType.COLLECTION_JSON,
			HypermediaType.UBER })
	static class AllHypermediaConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class CustomHypermediaConfig {

		@Bean
		HypermediaMappingInformation frodoMediaType() {
			return () -> Collections.singletonList(FRODO_JSON);
		}

	}

}
