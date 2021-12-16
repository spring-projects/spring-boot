/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.reactor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;

import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DebugAgentEnvironmentPostProcessor}.
 *
 * @author Brian Clozel
 */
@Disabled("We need the not-yet-released reactor-tools 3.4.11 for JDK 17 compatibility")
@ClassPathOverrides("io.projectreactor:reactor-tools:3.4.11")
class DebugAgentEnvironmentPostProcessorTests {

	static {
		MockEnvironment environment = new MockEnvironment();
		DebugAgentEnvironmentPostProcessor postProcessor = new DebugAgentEnvironmentPostProcessor();
		postProcessor.postProcessEnvironment(environment, null);
	}

	@Test
	void enablesReactorDebugAgent() {
		InstrumentedFluxProvider fluxProvider = new InstrumentedFluxProvider();
		Flux<Integer> flux = fluxProvider.newFluxJust();
		assertThat(Scannable.from(flux).stepName())
				.startsWith("Flux.just ⇢ at org.springframework.boot.reactor.InstrumentedFluxProvider.newFluxJust");
	}

}
