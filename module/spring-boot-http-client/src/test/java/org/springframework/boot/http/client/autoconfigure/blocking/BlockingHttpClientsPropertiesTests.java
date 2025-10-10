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

package org.springframework.boot.http.client.autoconfigure.blocking;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JettyClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ReactorClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.SimpleClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.blocking.BlockingHttpClientsProperties.Factory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BlockingHttpClientsProperties}.
 *
 * @author Phillip Webb
 */
class BlockingHttpClientsPropertiesTests {

	@Nested
	class FactoryTests {

		@Test
		void httpComponentsBuilder() {
			assertThat(Factory.HTTP_COMPONENTS.builder())
				.isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void jettyBuilder() {
			assertThat(Factory.JETTY.builder()).isInstanceOf(JettyClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void reactorBuilder() {
			assertThat(Factory.REACTOR.builder()).isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void jdkBuilder() {
			assertThat(Factory.JDK.builder()).isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void simpleBuilder() {
			assertThat(Factory.SIMPLE.builder()).isInstanceOf(SimpleClientHttpRequestFactoryBuilder.class);
		}

	}

}
