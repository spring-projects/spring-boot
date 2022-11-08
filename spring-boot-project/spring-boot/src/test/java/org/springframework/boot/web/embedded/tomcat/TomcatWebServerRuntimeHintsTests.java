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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatWebServer.TomcatWebServerRuntimeHints}.
 *
 * @author Sebastien Deleuze
 */
class TomcatWebServerRuntimeHintsTests {

	@Test
	void registersHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new TomcatWebServer.TomcatWebServerRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onMethod(AbstractProtocol.class, "setPort"))
				.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(AbstractProtocol.class, "setPortOffset"))
				.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(AbstractProtocol.class, "getPort"))
				.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(AbstractProtocol.class, "getPortOffset"))
				.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(AbstractProtocol.class, "getLocalPort"))
				.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(AbstractHttp11Protocol.class, "setMaxSavePostSize"))
				.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(AbstractHttp11Protocol.class, "setSecure"))
				.accepts(runtimeHints);
	}

}
