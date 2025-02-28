/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.http.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClientHttpRequestFactoryRuntimeHints}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class ClientHttpRequestFactoryRuntimeHintsTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		Field requestFactoryField = ReflectionUtils.findField(AbstractClientHttpRequestFactoryWrapper.class,
				"requestFactory");
		assertThat(requestFactoryField).isNotNull();
		assertThat(reflection.onField(requestFactoryField)).accepts(hints);
	}

	@Test
	void shouldRegisterHttpComponentHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection
			.onMethod(method(HttpComponentsClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
			.accepts(hints);
	}

	@Test
	void shouldRegisterJettyClientHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection.onMethod(method(JettyClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
			.accepts(hints);
		assertThat(reflection.onMethod(method(JettyClientHttpRequestFactory.class, "setReadTimeout", long.class)))
			.accepts(hints);
	}

	@Test
	void shouldRegisterReactorHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection.onMethod(method(ReactorClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
			.accepts(hints);
		assertThat(reflection.onMethod(method(ReactorClientHttpRequestFactory.class, "setReadTimeout", long.class)))
			.accepts(hints);
	}

	@Test
	void shouldRegisterSimpleHttpHints() {
		RuntimeHints hints = new RuntimeHints();
		new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection.onMethod(method(SimpleClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
			.accepts(hints);
		assertThat(reflection.onMethod(method(SimpleClientHttpRequestFactory.class, "setReadTimeout", int.class)))
			.accepts(hints);
	}

	private static Method method(Class<?> target, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(target, name, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

}
