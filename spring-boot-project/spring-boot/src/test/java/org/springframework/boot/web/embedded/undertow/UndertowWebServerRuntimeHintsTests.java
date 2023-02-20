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

package org.springframework.boot.web.embedded.undertow;

import io.undertow.Undertow;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates.FieldHintPredicate;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.web.embedded.undertow.UndertowWebServer.UndertowWebServerRuntimeHints;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UndertowWebServerRuntimeHints}.
 *
 * @author Andy Wilkinson
 */
class UndertowWebServerRuntimeHintsTests {

	@Test
	void registersHints() throws ClassNotFoundException {
		RuntimeHints runtimeHints = new RuntimeHints();
		new UndertowWebServerRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onField(Undertow.class, "listeners")).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onField(Undertow.class, "channels")).accepts(runtimeHints);
		assertThat(reflectionOnField("io.undertow.Undertow$ListenerConfig", "type")).accepts(runtimeHints);
		assertThat(reflectionOnField("io.undertow.Undertow$ListenerConfig", "port")).accepts(runtimeHints);
		assertThat(reflectionOnField("io.undertow.protocols.ssl.UndertowAcceptingSslChannel", "ssl"))
				.accepts(runtimeHints);
	}

	private FieldHintPredicate reflectionOnField(String className, String fieldName) throws ClassNotFoundException {
		return RuntimeHintsPredicates.reflection()
				.onField(ReflectionUtils.findField(Class.forName(className), fieldName));
	}

}
