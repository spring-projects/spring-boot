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

package org.springframework.boot.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClientHttpRequestFactoriesRuntimeHints}.
 *
 * @author Andy Wilkinson
 */
class ClientHttpRequestFactoriesRuntimeHintsTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoriesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection
				.onField(ReflectionUtils.findField(AbstractClientHttpRequestFactoryWrapper.class, "requestFactory")))
						.accepts(hints);
	}

	@Test
	void shouldRegisterHttpComponentHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoriesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection.onMethod(ReflectionUtils.findMethod(HttpComponentsClientHttpRequestFactory.class,
				"setConnectTimeout", int.class))).accepts(hints);
		assertThat(reflection.onMethod(
				ReflectionUtils.findMethod(HttpComponentsClientHttpRequestFactory.class, "setReadTimeout", int.class)))
						.accepts(hints);
		assertThat(reflection.onMethod(ReflectionUtils.findMethod(HttpComponentsClientHttpRequestFactory.class,
				"setBufferRequestBody", boolean.class))).accepts(hints);
	}

	@Test
	void shouldRegisterOkHttpHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoriesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection.onMethod(
				ReflectionUtils.findMethod(OkHttp3ClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
						.accepts(hints);
		assertThat(reflection.onMethod(
				ReflectionUtils.findMethod(OkHttp3ClientHttpRequestFactory.class, "setReadTimeout", int.class)))
						.accepts(hints);
	}

	@Test
	void shouldRegisterSimpleHttpHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoriesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection.onMethod(
				ReflectionUtils.findMethod(SimpleClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
						.accepts(hints);
		assertThat(reflection.onMethod(
				ReflectionUtils.findMethod(SimpleClientHttpRequestFactory.class, "setReadTimeout", int.class)))
						.accepts(hints);
		assertThat(reflection.onMethod(ReflectionUtils.findMethod(SimpleClientHttpRequestFactory.class,
				"setBufferRequestBody", boolean.class))).accepts(hints);
	}

}
